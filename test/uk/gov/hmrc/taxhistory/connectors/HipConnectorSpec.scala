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
import uk.gov.hmrc.http.{HeaderCarrier, RequestId}
import uk.gov.hmrc.taxhistory.model.nps._

import scala.concurrent.ExecutionContext.Implicits.global

class HipConnectorSpec extends BaseConnectorSpec {
  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()

  lazy val testHipNpsEmploymentAsString: String      =
    loadFile("/json/nps/response/hipEmployments.json").toString()
  lazy val testNpsEmployment: List[NpsEmployment]    =
    loadFile("/json/nps/response/hipEmployments.json").as[NpsEmployments].toListOfNpsEmployment
  lazy val testHipTaxAccountResponseAsString: String = loadFile("/json/nps/response/HIPGetTaxAccount.json").toString()
  lazy val testHipTaxAccount: NpsTaxAccount          = loadFile("/json/nps/response/HIPGetTaxAccount.json").as[NpsTaxAccount]
  lazy val testIabdResponseAsString: String          = loadFile("/json/nps/response/HIPiabds.json").toString()
  lazy val testIabd: List[Iabd]                      = loadFile("/json/nps/response/HIPiabds.json").as[IabdList].getListOfIabd
  lazy val uuid: String                              = "123f4567-g89c-42c3-b456-557742330000"
  lazy val hipNpsConnector: HipNpsConnector          = new HipNpsConnector(
    http = mockHttpClient,
    metrics = mockMetrics,
    config = mockAppConfig,
    system = system
  )
  lazy val hipNpsConnectorWithUUID: HipNpsConnector  = new HipNpsConnector(
    http = mockHttpClient,
    metrics = mockMetrics,
    config = mockAppConfig,
    system = system
  ) {
    override def generateNewUUID: String = uuid
  }
  val testNino: Nino                                 = randomNino()
  val testYear: Int                                  = 2016

  ".getCorrelationId" when {

    "requestID is present in the headerCarrier" should {
      "return new ID pre-appending the requestID when the requestID matches the format(8-4-4-4)" in {
        val requestId = "8c5d7809-0eec-4257-b4ad"
        hipNpsConnectorWithUUID.getCorrelationId(HeaderCarrier(requestId = Some(RequestId(requestId)))) mustBe
          s"$requestId-${uuid.substring(24)}"
      }

      "return new ID when the requestID does not match the format(8-4-4-4)" in {
        val requestId = "1a2b-ij12-df34-jk56"
        hipNpsConnectorWithUUID.getCorrelationId(HeaderCarrier(requestId = Some(RequestId(requestId)))) mustBe uuid
      }
    }

    "requestID is not present in the headerCarrier should return a new ID" should {
      "return the uuid" in {
        val uuid: String = "123f4567-g89c-42c3-b456-557742330000"
        hipNpsConnectorWithUUID.getCorrelationId(HeaderCarrier()) mustBe uuid
      }
    }
  }

  "create the correct hip headers" in {
    val headers = hipNpsConnectorWithUUID.buildHeaders(hc)
    headers mustBe List(
      ("gov-uk-originator-id", "MDTP-PAYE-TES-2"),
      ("correlationId", "123f4567-g89c-42c3-b456-557742330000"),
      ("Authorization", "Basic YXBpLWNsaWVudC1pZDphcGktY2xpZW50LXNlY3JldA==")
    )
  }

  "create the correct Hip url" when {
    "employment is read" in {
      hipNpsConnectorWithUUID.employmentsUrl(testNino, testYear) must be(
        s"http://localhost:9998/paye/employment/employee/$testNino/tax-year/$testYear/employment-details"
      )
    }
    "taxAccount is read" in {
      hipNpsConnectorWithUUID.taxAccountUrl(testNino, testYear) must be(
        s"http://localhost:9998/paye/person/$testNino/tax-account/$testYear"
      )
    }
  }

  "get EmploymentData data" when {
    "given a valid Nino and TaxYear" in {
      mockExecuteMethod(testHipNpsEmploymentAsString, OK)

      val result = hipNpsConnectorWithUUID.getEmployments(testNino, testYear)

      await(result) mustBe testNpsEmployment
    }

    "return an empty list if the response from HIP is 404 (Not Found)" in {
      mockExecuteMethod(NOT_FOUND)

      val result = hipNpsConnectorWithUUID.getEmployments(testNino, testYear)

      await(result) mustBe List.empty
    }
  }
  "get taxAccount data" when {
    "given a valid Nino and TaxYear" in {
      mockExecuteMethod(testHipTaxAccountResponseAsString, OK)

      val result = hipNpsConnectorWithUUID.getTaxAccount(testNino, testYear)

      await(result) mustBe Some(testHipTaxAccount)
    }

    "return an empty list if the response from HIP is 404 (Not Found)" in {
      mockExecuteMethod(NOT_FOUND)

      val result = hipNpsConnectorWithUUID.getTaxAccount(testNino, testYear)

      await(result) mustBe None
    }
  }
  "get Iabd data" when {
    "given a valid Nino and TaxYear" in {
      mockExecuteMethod(testIabdResponseAsString, OK)

      val result = hipNpsConnectorWithUUID.getIabds(testNino, testYear)

      await(result) mustBe testIabd
    }

    "return an empty list if the response from HIP is 404 (Not Found)" in {
      mockExecuteMethod(NOT_FOUND)

      val result = hipNpsConnectorWithUUID.getIabds(testNino, testYear)

      await(result) mustBe List.empty
    }
  }
}
