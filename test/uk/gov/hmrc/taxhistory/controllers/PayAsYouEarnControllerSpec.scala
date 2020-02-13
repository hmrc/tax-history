/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.taxhistory.controllers

import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.model.api.{Employment, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.{EmploymentHistoryService, SaAuthService}
import uk.gov.hmrc.taxhistory.utils.HttpErrors
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class PayAsYouEarnControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  val mockEmploymentHistoryService: EmploymentHistoryService = mock[EmploymentHistoryService]
  val mockSaAuthService: SaAuthService = mock[SaAuthService]

  val testNino = randomNino()
  val testEmploymentId = java.util.UUID.randomUUID
  val testStartDate = LocalDate.now()
  val testPaye =
    PayAsYouEarn(
      employments = List(Employment(
        employmentId = testEmploymentId,
        startDate = Some(testStartDate),
        payeReference = "SOME_PAYE", employerName = "Megacorp Plc",
        employmentStatus = EmploymentStatus.Live, worksNumber = "00191048716")),
      allowances = List.empty,
      incomeSources = Map.empty,
      benefits = Map.empty,
      payAndTax = Map.empty,
      taxAccount = None,
      statePension = None
    )

  override def beforeEach = {
    reset(mockEmploymentHistoryService)
    reset(mockSaAuthService)
  }

  val testCtrlr = new PayAsYouEarnController(mockEmploymentHistoryService, mockSaAuthService)

  def withSuccessfulSaAuthorisation(testCode: => Any) = {
    when(mockSaAuthService.checkSaAuthorisation(meq(testNino)))
      .thenReturn(Action)

    testCode
  }

  def withFailedSaAuthorisation(failureStatus: Int)(testCode: => Any) = {
    val failingAction = new ActionBuilder[Request] {
      override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]) = Future.successful(Results.Status(failureStatus))
    }
    when(mockSaAuthService.checkSaAuthorisation(meq(testNino)))
      .thenReturn(failingAction)

    testCode
  }

  def withSuccessfulGetFromCache(testCode: => Any) {
    when(mockEmploymentHistoryService.getFromCache(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(testPaye))

    testCode

    verify(mockEmploymentHistoryService).getFromCache(meq(testNino), any())(any[HeaderCarrier])
  }

  def withFailedGetFromCache(httpException: Exception)(testCode: => Any) = {
    def apply(testCode: => Any) = {
      when(mockEmploymentHistoryService.getFromCache(meq(testNino), any())(any[HeaderCarrier]))
        .thenReturn(Future.failed(httpException))

      testCode

      verify(mockEmploymentHistoryService).getFromCache(meq(testNino), any())(any[HeaderCarrier])
    }
  }

  "getAllDetails" must {

    "secure access via the SaAuthorisationService" when {
      "not logged in" in {
        withFailedSaAuthorisation(UNAUTHORIZED) {
          val result = testCtrlr.getPayAsYouEarn(testNino, TaxYear(2016))(FakeRequest())
          status(result) must be(UNAUTHORIZED)

          verify(mockSaAuthService).checkSaAuthorisation(meq(testNino))
          verifyZeroInteractions(mockEmploymentHistoryService)
        }
      }

      "not authorised to access the nino" in {
        withFailedSaAuthorisation(FORBIDDEN) {
          val result = testCtrlr.getPayAsYouEarn(testNino, TaxYear(2016))(FakeRequest())
          status(result) must be(FORBIDDEN)

          verify(mockSaAuthService).checkSaAuthorisation(meq(testNino))
          verifyZeroInteractions(mockEmploymentHistoryService)
        }
      }

      "logged in and authorised to access the nino" in {
        withSuccessfulSaAuthorisation {
          withSuccessfulGetFromCache {
            val result = testCtrlr.getPayAsYouEarn(testNino, TaxYear(2016))(FakeRequest())
            status(result) must be(OK)

            verify(mockSaAuthService).checkSaAuthorisation(meq(testNino))
          }
        }
      }
    }

    "respond with OK for successful get" in {
      withSuccessfulSaAuthorisation {
        withSuccessfulGetFromCache {
          val result = testCtrlr.getPayAsYouEarn(testNino, TaxYear(2016)).apply(FakeRequest())
          status(result) must be(OK)
        }
      }
    }

    "respond with json serialised PayAsYouEarn" in {
      withSuccessfulSaAuthorisation {
        withSuccessfulGetFromCache {
          val result = testCtrlr.getPayAsYouEarn(testNino, TaxYear(2016)).apply(FakeRequest())
          contentAsJson(result) must be(Json.parse(
            s"""
               |{
               |  "employments" : [
               |    {
               |      "employmentId":"${testEmploymentId.toString}",
               |      "startDate":"${testStartDate.toString}",
               |      "payeReference":"SOME_PAYE",
               |      "employerName":"Megacorp Plc",
               |      "employmentStatus":1,
               |      "worksNumber":"00191048716"
               |    }
               |  ],
               |  "allowances" : [],
               |  "incomeSources" : {},
               |  "benefits" : {},
               |  "payAndTax" : {}
               |}
          """.stripMargin))
        }
      }
    }

    HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
      s"propagate error responses from upstream microservices: when exception is ${httpException.getClass.getSimpleName} and expected status is $expectedStatus" in {
        withSuccessfulSaAuthorisation {
          withFailedGetFromCache(httpException) {
            val result = testCtrlr.getPayAsYouEarn(testNino, TaxYear(2015)).apply(FakeRequest())
            status(result) must be(expectedStatus)
          }
        }
      }
    }
  }
}
