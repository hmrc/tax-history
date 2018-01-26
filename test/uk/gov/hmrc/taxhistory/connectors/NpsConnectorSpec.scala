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

package uk.gov.hmrc.taxhistory.connectors

import com.codahale.metrics.Timer
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.{BadRequest, GenericHttpError, ServiceUnavailable, TaxHistoryException}

import scala.concurrent.Future


class NpsConnectorSpec extends PlaySpec with MockitoSugar with TestUtil {

  lazy val employmentsSuccessfulResponseJson = loadFile("/json/nps/response/employments.json")
  lazy val employmentsSuccessfulResponse = employmentsSuccessfulResponseJson.as[List[NpsEmployment]]

  lazy val iabdsSuccessfulResponseJson = loadFile("/json/nps/response/iabds.json")
  lazy val iabdsSuccessfulResponse = iabdsSuccessfulResponseJson.as[List[Iabd]]

  lazy val taxAccountResponseJson = loadFile("/json/nps/response/GetTaxAccount.json")
  lazy val taxAccountResponse = taxAccountResponseJson.as[NpsTaxAccount]


  val mockServicesConfig = mock[ServicesConfig]
  when(mockServicesConfig.baseUrl(any[String])).thenReturn("/test")

  val testNino = randomNino()
  val testYear = 2016

  "EmploymentConnector" should {

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        testNpsConnector.withoutSuffix(testNino) mustBe s"${testNino.value.take(8)}"
      }
    }

    "create the correct headers" in {
      val headers = testNpsConnector.basicNpsHeaders(HeaderCarrier())
      headers.extraHeaders mustBe List(("Gov-Uk-Originator-Id", "orgId"))
    }

    "get EmploymentData data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testemploymentsConnector = testNpsConnector
        val fakeResponse: HttpResponse = HttpResponse(OK, Some(employmentsSuccessfulResponseJson))

        when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testemploymentsConnector.httpGet.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(fakeResponse))

        val result = testemploymentsConnector.getEmployments(testNino, testYear)

        await(result) mustBe employmentsSuccessfulResponse
      }

      "return and handle an error response" in {
        val expectedResponse = Json.parse( """{"reason": "Some thing went wrong"}""")
        implicit val hc = HeaderCarrier()
        val testemploymentsConnector = testNpsConnector
        when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testemploymentsConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(expectedResponse))))

        val result = testemploymentsConnector.getEmployments(testNino, testYear)

        intercept[Exception] (await(result)) must matchPattern {
          case TaxHistoryException(BadRequest, _) =>
        }
      }
    }



    "get Iabds data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testIabdsConnector = testNpsConnector
        val fakeResponse: HttpResponse = HttpResponse(OK, Some(iabdsSuccessfulResponseJson))

        when(testIabdsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testIabdsConnector.httpGet.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(fakeResponse))

        val result = testIabdsConnector.getIabds(testNino, testYear)

        await(result) mustBe iabdsSuccessfulResponse
      }

      "return and handle an service unavailable error response " in {
        val expectedResponse = Json.parse( """{"reason": "Service Unavailable Error"}""")
        implicit val hc = HeaderCarrier()
        val testIabdsConnector = testNpsConnector
        when(testIabdsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testIabdsConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(expectedResponse))))

        val result = testIabdsConnector.getIabds(testNino, testYear)

        intercept[Exception](await(result)) must matchPattern {
          case TaxHistoryException(ServiceUnavailable, _) =>
        }
      }
    }

    "get Tax Account data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testTaxAccountConnector = testNpsConnector
        val fakeResponse: HttpResponse = HttpResponse(OK, Some(taxAccountResponseJson))

        when(testTaxAccountConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testTaxAccountConnector.httpGet.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(fakeResponse))

        val result = testTaxAccountConnector.getTaxAccount(testNino, testYear)

        await(result) mustBe taxAccountResponse
      }

      "return and handle an error response" in {
        val expectedResponse = Json.parse( """{"reason": "Some thing went wrong"}""")
        implicit val hc = HeaderCarrier()
        val testTaxAccountConnector = testNpsConnector
        when(testTaxAccountConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testTaxAccountConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.failed(TaxHistoryException.badRequest))

        val result = testTaxAccountConnector.getTaxAccount(testNino, testYear)

        intercept[Exception](await(result)) must matchPattern {
          case TaxHistoryException(BadRequest, _) =>
        }
      }
    }

  }

  lazy val testNpsConnector = new NpsConnector(
    httpGet = mock[HttpGet],
    httpPost = mock[HttpPost],
    audit = mock[Audit],
    servicesConfig = mockServicesConfig,
    originatorId = "orgId",
    path = "/path"
  ) {
    override val metrics = mock[TaxHistoryMetrics]
    val mockTimerContext = mock[Timer.Context]
  }

}


