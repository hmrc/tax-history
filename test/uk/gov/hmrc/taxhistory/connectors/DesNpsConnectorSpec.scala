/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, NpsTaxAccount}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DesNpsConnectorSpec extends BaseConnectorSpec {

  lazy val testIabds: List[Iabd]                  = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]
  lazy val testIabdsAsString: String              = loadFile("/json/nps/response/iabds.json").toString()
  lazy val testNpsTaxAccount: NpsTaxAccount       = loadFile("/json/nps/response/GetTaxAccount.json").as[NpsTaxAccount]
  lazy val testNpsTaxAccountAsString: String      = loadFile("/json/nps/response/GetTaxAccount.json").toString()
  lazy val testNpsEmployment: List[NpsEmployment] =
    loadFile("/json/nps/response/employments.json").as[List[NpsEmployment]]
  lazy val testNpsEmploymentAsString: String      =
    loadFile("/json/nps/response/employments.json").toString()

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

  "return new ID pre-appending the requestID when the requestID matches the format(8-4-4-4)" in {
    val requestId  = "123f4567-g89c-42c3-b456"
    val beginIndex = 24
    testDesNpsConnector.getCorrelationId(HeaderCarrier(requestId = Some(RequestId(requestId)))) shouldBe
      s"$requestId-${uuid.substring(beginIndex)}"
  }

  "return new ID when the requestID does not match the format(8-4-4-4)" in {
    val requestId = "1a2b-ij12-df34-jk56"
    testDesNpsConnector.getCorrelationId(HeaderCarrier(requestId = Some(RequestId(requestId)))) shouldBe uuid
  }

  "return the new uuid when requestID is not present in the headerCarrier" in {
    testDesNpsConnector.getCorrelationId(HeaderCarrier()) shouldBe uuid
  }

  "EmploymentConnector have withoutSuffix nino" when {
    "given a valid nino" in {
      testNino.withoutSuffix mustBe s"${testNino.value.take(8)}"
    }
  }

  "create the correct headers" in {
    val headers = testDesNpsConnector.buildHeaders(hc)
    headers mustBe List(
      ("Environment", "local"),
      ("Authorization", "Bearer local"),
      ("CorrelationId", "123f4567-g89c-42c3-b456-557742330000")
    )
  }

  "create the correct url for iabds" in {
    testDesNpsConnector.iabdsUrl(testNino, testYear) must be(
      s"http://localhost:9998/pay-as-you-earn/individuals/$testNino/iabds/tax-year/$testYear"
    )
  }

  "create the correct url for taxAccount" in {
    testDesNpsConnector.taxAccountUrl(testNino, testYear) must be(
      s"http://localhost:9998/pay-as-you-earn/individuals/$testNino/tax-account/tax-year/$testYear"
    )
  }

  "create the correct url for employment" in {
    testDesNpsConnector.employmentsUrl(testNino, testYear) must be(
      s"http://localhost:9998/individuals/$testNino/employment/$testYear"
    )
  }

  "get Iabds data " when {
    "given a valid Nino and TaxYear" in {
      mockExecuteMethod(testIabdsAsString, OK)

      val result = testDesNpsConnector.getIabds(testNino, testYear)

      await(result) mustBe testIabds
    }

    "retrying after the first call fails and the second call succeeds" in {
      when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
        .thenReturn(Future.successful(buildHttpResponse(SERVICE_UNAVAILABLE)))
        .thenReturn(Future.successful(buildHttpResponse(testIabdsAsString)))

      val result = testDesNpsConnector.getIabds(testNino, testYear)

      await(result) mustBe testIabds
    }

    "return empty list when the call to get IABD returns 404 NotFoundException" in {
      mockExecuteMethod(NOT_FOUND)

      val result = testDesNpsConnector.getIabds(testNino, testYear)

      await(result).isEmpty mustBe true
    }

    "return and handle an service unavailable error response " in {
      mockExecuteMethod(SERVICE_UNAVAILABLE)

      val result = testDesNpsConnector.getIabds(testNino, testYear)

      intercept[UpstreamErrorResponse](await(result))
    }
  }

  "get Tax Account data " when {
    "given a valid Nino and TaxYear" in {
      mockExecuteMethod(testNpsTaxAccountAsString, OK)

      val result = testDesNpsConnector.getTaxAccount(testNino, testYear)

      await(result) mustBe Some(testNpsTaxAccount)
    }

    "retrying after the first call fails and the second call succeeds" in {
      when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
        .thenReturn(Future.successful(buildHttpResponse(SERVICE_UNAVAILABLE)))
        .thenReturn(Future.successful(buildHttpResponse(testNpsTaxAccountAsString)))

      val result = testDesNpsConnector.getTaxAccount(testNino, testYear)

      await(result) mustBe Some(testNpsTaxAccount)
    }

    "return and handle an error response" in {
      mockExecuteMethod(BAD_REQUEST)

      val result = testDesNpsConnector.getTaxAccount(testNino, testYear)

      intercept[UpstreamErrorResponse](await(result))
    }

    "return None if the response from DES is 404 (NotFound)" in {
      mockExecuteMethod(NOT_FOUND)

      val result = testDesNpsConnector.getTaxAccount(testNino, testYear)

      await(result) mustBe None
    }
  }

  "get EmploymentData data" when {
    "given a valid Nino and TaxYear" in {
      mockExecuteMethod(testNpsEmploymentAsString, OK)

      val result = testDesNpsConnector.getEmployments(testNino, testYear)

      await(result) mustBe testNpsEmployment
    }

    "retrying after the first call fails and the second call succeeds" in {
      when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
        .thenReturn(Future.successful(buildHttpResponse(INTERNAL_SERVER_ERROR)))
        .thenReturn(Future.successful(buildHttpResponse(testNpsEmploymentAsString)))

      val result = testDesNpsConnector.getEmployments(testNino, testYear)

      await(result) mustBe testNpsEmployment
    }

    "return and handle an error response" in {
      mockExecuteMethod(BAD_REQUEST)

      val result = testDesNpsConnector.getEmployments(testNino, testYear)

      intercept[UpstreamErrorResponse](await(result))
    }

    "return an empty list if the response from DES is 404 (Not Found)" in {
      mockExecuteMethod(NOT_FOUND)

      val result = testDesNpsConnector.getEmployments(testNino, testYear)

      await(result) mustBe List.empty
    }
  }

  "generateNewUUID" in {
    val uuidLength = 36
    val id         = desNpsConnector.generateNewUUID

    id     shouldNot be(empty)
    id.length should be(uuidLength)
  }
}
