/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.connectors

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.taxhistory.model.nps.HIPNpsEmployments.toListOfHIPNpsEmployment
import uk.gov.hmrc.taxhistory.model.nps.{HIPNpsEmployment, HIPNpsEmployments, NpsEmployment}
import scala.concurrent.ExecutionContext.Implicits.global

class HipConnectorSpec extends BaseConnectorSpec {
  override lazy val app: Application = new GuiceApplicationBuilder().configure(configForHip).build()

  lazy val testHipNpsEmployment: List[NpsEmployment] =
    toListOfHIPNpsEmployment(loadFile("/json/nps/response/hipEmployments.json").as[HIPNpsEmployments])
      .map[NpsEmployment](HIPNpsEmployment.toNpsEmployment)
  lazy val testNpsEmploymentAsString: String         =
    loadFile("/json/nps/response/hipEmployments.json").toString()

  lazy val uuid: String                         = "123f4567-g89c-42c3-b456-557742330000"
  lazy val desNpsConnector: DesNpsConnector     = new DesNpsConnector(
    http = mockHttpClient,
    metrics = mockMetrics,
    config = mockAppConfig,
    system = system
  )
  lazy val testDesNpsConnector: DesNpsConnector = new DesNpsConnector(
    http = mockHttpClient,
    metrics = mockMetrics,
    config = mockAppConfig,
    system = system
  ) {
    override def generateNewUUID: String = uuid
  }
  val testNino: Nino                            = randomNino()
  val testYear: Int                             = 2016

  "create the correct hip headers" in {
    val headers = testDesNpsConnector.buildHIPHeaders(hc)
    headers mustBe List(
      ("correlationId", "123f4567-g89c-42c3-b456-557742330000"),
      ("gov-uk-originator-id", "testId")
    )
  }

  "create the correct Hip url for employment" in {
    testDesNpsConnector.employmentsHIPUrl(testNino, testYear) must be(
      s"http://localhost:9998/employment/employee/$testNino/tax-year/$testYear/employment-details"
    )
  }

  "get EmploymentData data" when {
    "given a valid Nino and TaxYear" in {
      mockExecuteMethod(testNpsEmploymentAsString, OK)

      val result = testDesNpsConnector.getEmployments(testNino, testYear)

      await(result) mustBe testHipNpsEmployment
    }

    /*    "retrying after the first call fails and the second call succeeds" in {
      when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
        .thenReturn(Future.successful(buildHttpResponse(INTERNAL_SERVER_ERROR)))
        .thenReturn(Future.successful(buildHttpResponse(testNpsEmploymentAsString)))

      val result = testDesNpsConnector.getEmployments(testNino, testYear)

      await(result) mustBe testHipNpsEmployment
    }

    "return and handle an error response" in {
      mockExecuteMethod(BAD_REQUEST)

      val result = testDesNpsConnector.getEmployments(testNino, testYear)

      intercept[UpstreamErrorResponse](await(result))
    }
     */
    "return an empty list if the response from DES is 404 (Not Found)" in {
      mockExecuteMethod(NOT_FOUND)

      val result = testDesNpsConnector.getEmployments(testNino, testYear)

      await(result) mustBe List.empty
    }
  }
}
