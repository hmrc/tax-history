/*
 * Copyright 2018 HM Revenue & Customs
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
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.model.api.{Employment, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.taxhistory.utils.{HttpErrors, TestRelationshipAuthService}

import scala.concurrent.Future

class PayAsYouEarnControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  val mockEmploymentHistoryService: EmploymentHistoryService = mock[EmploymentHistoryService]

  val ninoWithAgent = randomNino()
  val ninoWithoutAgent = randomNino()

  val testEmploymentId = java.util.UUID.randomUUID
  val testStartDate = LocalDate.now()
  val testPaye =
    PayAsYouEarn(
      employments = List(Employment(
        employmentId = testEmploymentId,
        startDate = testStartDate,
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
  }

  val testCtrlr = new PayAsYouEarnController(mockEmploymentHistoryService)

  private trait GetFromCacheSucceeds {
    when(mockEmploymentHistoryService.getFromCache(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
      .thenReturn(Future.successful(testPaye))
  }

  private class GetFromCacheFails(httpException: Exception) {
    when(mockEmploymentHistoryService.getFromCache(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
      .thenReturn(Future.failed(httpException))
  }

  "getAllDetails" must {

    "respond with OK for successful get" in new GetFromCacheSucceeds {
      val result = testCtrlr.getPayAsYouEarn(ninoWithAgent.nino, taxYear = 2016).apply(FakeRequest())
      status(result) must be(OK)
    }

    "respond with json serialised PayAsYouEarn" in new GetFromCacheSucceeds {
      val result = testCtrlr.getPayAsYouEarn(ninoWithAgent.nino, taxYear = 2016).apply(FakeRequest())
      contentAsJson(result) must be(Json.parse(
        s"""
          |{
          |  "employments" : [
          |    {
          |      "employmentId":"${testEmploymentId.toString}",
          |      "startDate":"${testStartDate.toString}",
          |      "payeReference":"SOME_PAYE",
          |      "employerName":"Megacorp Plc",
          |      "receivingOccupationalPension":false,
          |      "receivingJobSeekersAllowance":false,
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

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        new GetFromCacheFails(httpException) {
          val result = testCtrlr.getPayAsYouEarn(ninoWithAgent.nino, taxYear = 2015).apply(FakeRequest())
          status(result) must be(expectedStatus)
        }
      }
    }

  }
}
