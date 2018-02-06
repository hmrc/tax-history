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
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

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
        testNino.withoutSuffix mustBe s"${testNino.value.take(8)}"
      }
    }

    "create the correct headers" in {
      val headers = testNpsConnector.basicNpsHeaders(HeaderCarrier())
      headers.extraHeaders mustBe List(("Gov-Uk-Originator-Id", "orgId"))
    }

    "get EmploymentData data" when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testemploymentsConnector = testNpsConnector

        when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testemploymentsConnector.http.GET[List[NpsEmployment]](any())(any(), any(), any())).thenReturn(Future.successful(employmentsSuccessfulResponse))

        val result = testemploymentsConnector.getEmployments(testNino, testYear)

        await(result) mustBe employmentsSuccessfulResponse
      }

      "return and handle an error response" in {
        val expectedResponse = Json.parse( """{"reason": "Some thing went wrong"}""")
        implicit val hc = HeaderCarrier()
        val testemploymentsConnector = testNpsConnector
        when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testemploymentsConnector.http.GET[List[NpsEmployment]](any())(any(), any(), any()))
          .thenReturn(Future.failed(new BadRequestException("")))

        val result = testemploymentsConnector.getEmployments(testNino, testYear)

        intercept[BadRequestException](await(result))
      }
    }

    "get Iabds data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testIabdsConnector = testNpsConnector

        when(testIabdsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testIabdsConnector.http.GET[List[Iabd]](any())(any(), any(), any())).thenReturn(Future.successful(iabdsSuccessfulResponse))

        val result = testIabdsConnector.getIabds(testNino, testYear)

        await(result) mustBe iabdsSuccessfulResponse
      }

      "return and handle an service unavailable error response " in {
        val expectedResponse = Json.parse( """{"reason": "Service Unavailable Error"}""")
        implicit val hc = HeaderCarrier()
        val testIabdsConnector = testNpsConnector
        when(testIabdsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testIabdsConnector.http.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.failed(new Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

        val result = testIabdsConnector.getIabds(testNino, testYear)

        intercept[Upstream5xxResponse](await(result))
      }
    }

    "get Tax Account data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testTaxAccountConnector = testNpsConnector

        when(testTaxAccountConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testTaxAccountConnector.http.GET[NpsTaxAccount](any())(any(), any(), any())).thenReturn(Future.successful(taxAccountResponse))

        val result = testTaxAccountConnector.getTaxAccount(testNino, testYear)

        await(result) mustBe taxAccountResponse
      }

      "return and handle an error response" in {
        val expectedResponse = Json.parse( """{"reason": "Some thing went wrong"}""")
        implicit val hc = HeaderCarrier()
        val testTaxAccountConnector = testNpsConnector
        when(testTaxAccountConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testTaxAccountConnector.http.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.failed(new BadRequestException("")))

        val result = testTaxAccountConnector.getTaxAccount(testNino, testYear)

        intercept[BadRequestException](await(result))
      }
    }

  }

  lazy val testNpsConnector = new NpsConnector(
    http = mock[HttpGet],
    audit = mock[Audit],
    serviceUrl = "/fake",
    metrics = mock[TaxHistoryMetrics],
    originatorId = "orgId",
    path = "/path"
  ) {
    override val metrics = mock[TaxHistoryMetrics]
    val mockTimerContext = mock[Timer.Context]
  }

}


