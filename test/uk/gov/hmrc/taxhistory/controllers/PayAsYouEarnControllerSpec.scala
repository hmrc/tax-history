/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.connectors.CitizenDetailsConnector
import uk.gov.hmrc.taxhistory.model.api.PayAsYouEarn
import uk.gov.hmrc.taxhistory.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.{EmploymentHistoryService, SaAuthService}
import uk.gov.hmrc.taxhistory.utils.{HttpErrors, TestSaAuthService}
import uk.gov.hmrc.time.TaxYear

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class PayAsYouEarnControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  val cc: ControllerComponents = stubControllerComponents()
  implicit val executionContext: ExecutionContext = cc.executionContext
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testSaAuthService: TestSaAuthService = TestSaAuthService()

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockEmploymentHistoryService: EmploymentHistoryService = mock[EmploymentHistoryService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockSaAuthService: SaAuthService = mock[SaAuthService]

  val testEmploymentId: UUID = testSaAuthService.testEmploymentId
  val testStartDate: LocalDate = testSaAuthService.testStartDate
  val testPaye: PayAsYouEarn = testSaAuthService.testPaye
  val validNino: Nino = testSaAuthService.validNino
  val unauthorisedNino: Nino = testSaAuthService.unauthorisedNino
  val forbiddenNino: Nino = testSaAuthService.forbiddenNino

  override def beforeEach: Unit = {
    reset(mockEmploymentHistoryService)
  }

  val testCtrlr = new PayAsYouEarnController(mockEmploymentHistoryService, testSaAuthService, cc)

  def withSuccessfulGetFromCache(testCode: => Any): Unit = {
    when(mockEmploymentHistoryService.getFromCache(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(testPaye))
    testCode
  }

  def withFailedGetFromCache(httpException: Exception)(testCode: => Any): Unit = {
    def apply(testCode: => Any) = {
      when(mockEmploymentHistoryService.getFromCache(meq(unauthorisedNino), any())(any[HeaderCarrier]))
        .thenReturn(Future.failed(httpException))

      testCode
    }
  }

  "getAllDetails" must {

    "secure access via the SaAuthorisationService" when {
      "not logged in" in {
        {
          val result = testCtrlr.getPayAsYouEarn(unauthorisedNino, TaxYear(2016)).apply(FakeRequest())
          status(result) must be(UNAUTHORIZED)

          //verify(mockSaAuthService).saAuthValidator
          verifyNoInteractions(mockEmploymentHistoryService)
        }
      }

      "not authorised to access the nino" in {
        {
          val result = testCtrlr.getPayAsYouEarn(forbiddenNino, TaxYear(2016)).apply(FakeRequest())
          status(result) must be(FORBIDDEN)

          //verify(mockSaAuthService).saAuthValidator
          verifyNoInteractions(mockEmploymentHistoryService)
        }
      }

      "logged in and authorised to access the nino" in {
        val result = testCtrlr.getPayAsYouEarn(validNino, TaxYear(2016)).apply(FakeRequest())
        status(result) must be(OK)

        //verify(mockSaAuthService).saAuthValidator
      }
    }

    "respond with OK for successful get" in {
      {
        withSuccessfulGetFromCache {
          val result = testCtrlr.getPayAsYouEarn(validNino, TaxYear(2016)).apply(FakeRequest())
          status(result) must be(OK)
        }
      }
    }

    "respond with json serialised PayAsYouEarn" in {
      {
        withSuccessfulGetFromCache {
          val result = testCtrlr.getPayAsYouEarn(validNino, TaxYear(2016)).apply(FakeRequest())
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
        {
          withFailedGetFromCache(httpException) {
            val result = testCtrlr.getPayAsYouEarn(validNino, TaxYear(2015)).apply(FakeRequest())
            status(result) must be(expectedStatus)
          }
        }
      }
    }
  }
}
