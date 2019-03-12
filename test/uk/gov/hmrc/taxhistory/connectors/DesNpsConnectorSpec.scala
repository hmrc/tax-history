/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.codahale.metrics.Timer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.utils.Retry

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class DesNpsConnectorSpec extends PlaySpec with MockitoSugar with TestUtil {

  lazy val testIabds = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]
  lazy val testNpsTaxAccount = loadFile("/json/nps/response/GetTaxAccount.json").as[NpsTaxAccount]
  lazy val testNpsEmployment = loadFile("/json/nps/response/employments.json").as[List[NpsEmployment]]

  val testNino = randomNino()
  val testYear = 2016

  "EmploymentConnector" should {

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        testNino.withoutSuffix mustBe s"${testNino.value.take(8)}"
      }
    }

    "create the correct headers" in {
      val headers = testDesNpsConnector.basicDesHeaders(HeaderCarrier())
      headers.extraHeaders mustBe List(("Environment", "test"), ("Authorization", "Bearer someToken"))
    }

    "create the correct url for iabds" in {
      testDesNpsConnector.iabdsUrl(testNino, testYear) must be(s"/fake/pay-as-you-earn/individuals/$testNino/iabds/tax-year/$testYear")
    }

    "create the correct url for taxAccount" in {
      testDesNpsConnector.taxAccountUrl(testNino, testYear) must be(s"/fake/pay-as-you-earn/individuals/$testNino/tax-account/tax-year/$testYear")
    }

    "create the correct url for employment" in {
      testDesNpsConnector.employmentsUrl(testNino, testYear) must be (s"/fake/pay-as-you-earn/individuals/$testNino/employment/$testYear")
    }

    "get Iabds data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()

        when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testDesNpsConnector.http.GET[List[Iabd]](any())(any(), any(), any()))
          .thenReturn(Future.successful(testIabds))

        val result = testDesNpsConnector.getIabds(testNino, testYear)

        await(result) mustBe testIabds
      }

      "retrying after the first call fails and the second call succeeds" in {
        implicit val hc = HeaderCarrier()

        when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testDesNpsConnector.http.GET[List[Iabd]](any())(any(), any(), any()))
          .thenReturn(Future.failed(new Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))
          .thenReturn(Future.successful(testIabds))

        val result = testDesNpsConnector.getIabds(testNino, testYear)

        await(result) mustBe testIabds
      }

      "return empty list when the call to get IABD returns 404 NotFoundException" in {
        implicit val hc = HeaderCarrier()

        when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testDesNpsConnector.http.GET[List[Iabd]](any())(any(), any(), any()))
          .thenReturn(Future.failed(new NotFoundException("")))

        val result = testDesNpsConnector.getIabds(testNino, testYear)

        await(result).isEmpty mustBe true
      }


      "return and handle an service unavailable error response " in {
        implicit val hc = HeaderCarrier()
        when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testDesNpsConnector.http.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.failed(new Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

        val result = testDesNpsConnector.getIabds(testNino, testYear)

        intercept[Upstream5xxResponse](await(result))
      }
    }

    "get Tax Account data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testTaxAccountConnector = testDesNpsConnector

        when(testTaxAccountConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testTaxAccountConnector.http.GET[NpsTaxAccount](any())(any(), any(), any()))
          .thenReturn(Future.successful(testNpsTaxAccount))

        val result = testTaxAccountConnector.getTaxAccount(testNino, testYear)

        await(result) mustBe Some(testNpsTaxAccount)
      }

      "retrying after the first call fails and the second call succeeds" in {
        implicit val hc = HeaderCarrier()
        val testTaxAccountConnector = testDesNpsConnector

        when(testTaxAccountConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testTaxAccountConnector.http.GET[NpsTaxAccount](any())(any(), any(), any()))
          .thenReturn(Future.failed(new Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))
          .thenReturn(Future.successful(testNpsTaxAccount))

        val result = testTaxAccountConnector.getTaxAccount(testNino, testYear)

        await(result) mustBe Some(testNpsTaxAccount)
      }

      "return and handle an error response" in {
        implicit val hc = HeaderCarrier()
        when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testDesNpsConnector.http.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.failed(new BadRequestException("")))

        val result = testDesNpsConnector.getTaxAccount(testNino, testYear)

        intercept[BadRequestException](await(result))
      }

      "return None if the response from DES is 404 (NotFound)" in {
        implicit val hc = HeaderCarrier()
        when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testDesNpsConnector.http.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.failed(new NotFoundException("")))

        val result = testDesNpsConnector.getTaxAccount(testNino, testYear)

        await(result) mustBe None
      }
    }

    "get EmploymentData data" when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testemploymentsConnector = testDesNpsConnector

        when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testemploymentsConnector.http.GET[List[NpsEmployment]](any())(any(), any(), any()))
          .thenReturn(Future.successful(testNpsEmployment))

        val result = testemploymentsConnector.getEmployments(testNino, testYear)

        await(result) mustBe testNpsEmployment
      }

      "retrying after the first call fails and the second call succeeds" in {
        implicit val hc = HeaderCarrier()
        val testemploymentsConnector = testDesNpsConnector

        when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testemploymentsConnector.http.GET[List[NpsEmployment]](any())(any(), any(), any()))
          .thenReturn(Future.failed(new Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
          .thenReturn(Future.successful(testNpsEmployment))

        val result = testemploymentsConnector.getEmployments(testNino, testYear)

        await(result) mustBe testNpsEmployment
      }

      "return and handle an error response" in {
        implicit val hc = HeaderCarrier()
        val testemploymentsConnector = testDesNpsConnector
        when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testemploymentsConnector.http.GET[List[NpsEmployment]](any())(any(), any(), any()))
          .thenReturn(Future.failed(new BadRequestException("")))

        val result = testemploymentsConnector.getEmployments(testNino, testYear)

        intercept[BadRequestException](await(result))
      }
    }


  }

  private val system = ActorSystem("test")
  private val delay = FiniteDuration(500, TimeUnit.MILLISECONDS)

  lazy val testDesNpsConnector = new DesNpsConnector(
    http = mock[HttpClient],
    baseUrl = "/fake",
    metrics = mock[TaxHistoryMetrics],
    authorizationToken = "someToken",
    env = "test", withRetry = new Retry(1, delay, system)
  ) {
    override val metrics = mock[TaxHistoryMetrics]
    val mockTimerContext = mock[Timer.Context]
  }
}
