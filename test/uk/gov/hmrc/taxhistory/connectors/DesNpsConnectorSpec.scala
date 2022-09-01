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

package uk.gov.hmrc.taxhistory.connectors

import akka.actor.ActorSystem
import com.codahale.metrics.Timer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HttpClient, _}
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.utils.TestUtil
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DesNpsConnectorSpec extends PlaySpec with MockitoSugar with TestUtil with GuiceOneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()

  lazy val testIabds: List[Iabd]                  = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]
  lazy val testNpsTaxAccount: NpsTaxAccount       = loadFile("/json/nps/response/GetTaxAccount.json").as[NpsTaxAccount]
  lazy val testNpsEmployment: List[NpsEmployment] =
    loadFile("/json/nps/response/employments.json").as[List[NpsEmployment]]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockHttpClient        = mock[HttpClient]
  private val mockTaxHistoryMetrics = mock[TaxHistoryMetrics]
  private val mockAppConfig         = app.injector.instanceOf[AppConfig]
  private val system                = ActorSystem("test")

  val uuid                     = "123f4567-g89c-42c3-b456-557742330000"
  lazy val testDesNpsConnector = new DesNpsConnector(
    http = mockHttpClient,
    metrics = mockTaxHistoryMetrics,
    config = mockAppConfig,
    system = system
  ) {
    override def generateNewUUID: String = uuid
  }
  val testNino: Nino           = randomNino()
  val testYear                 = 2016

  "return new ID pre-appending the requestID when the requestID matches the format(8-4-4-4)" in {
    val requestId  = "dcba0000-ij12-df34-jk56"
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
      // scalastyle:off magic.number
      testNino.withoutSuffix mustBe s"${testNino.value.take(8)}"
      // scalastyle:on magic.number
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
      when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testDesNpsConnector.http.GET[List[Iabd]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(testIabds))

      val result = testDesNpsConnector.getIabds(testNino, testYear)

      await(result) mustBe testIabds
    }

    "retrying after the first call fails and the second call succeeds" in {
      when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testDesNpsConnector.http.GET[List[Iabd]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))
        .thenReturn(Future.successful(testIabds))

      val result = testDesNpsConnector.getIabds(testNino, testYear)

      await(result) mustBe testIabds
    }

    "return empty list when the call to get IABD returns 404 NotFoundException" in {
      when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testDesNpsConnector.http.GET[List[Iabd]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Not found", NOT_FOUND, NOT_FOUND)))

      val result = testDesNpsConnector.getIabds(testNino, testYear)

      await(result).isEmpty mustBe true
    }

    "return and handle an service unavailable error response " in {
      when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testDesNpsConnector.http.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = testDesNpsConnector.getIabds(testNino, testYear)

      intercept[UpstreamErrorResponse](await(result))
    }
  }

  "get Tax Account data " when {
    "given a valid Nino and TaxYear" in {
      val testTaxAccountConnector = testDesNpsConnector

      when(testTaxAccountConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testTaxAccountConnector.http.GET[NpsTaxAccount](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(testNpsTaxAccount))

      val result = testTaxAccountConnector.getTaxAccount(testNino, testYear)

      await(result) mustBe Some(testNpsTaxAccount)
    }

    "retrying after the first call fails and the second call succeeds" in {
      val testTaxAccountConnector = testDesNpsConnector

      when(testTaxAccountConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testTaxAccountConnector.http.GET[NpsTaxAccount](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))
        .thenReturn(Future.successful(testNpsTaxAccount))

      val result = testTaxAccountConnector.getTaxAccount(testNino, testYear)

      await(result) mustBe Some(testNpsTaxAccount)
    }

    "return and handle an error response" in {

      when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testDesNpsConnector.http.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      val result = testDesNpsConnector.getTaxAccount(testNino, testYear)

      intercept[BadRequestException](await(result))
    }

    "return None if the response from DES is 404 (NotFound)" in {

      when(testDesNpsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testDesNpsConnector.http.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Not found", NOT_FOUND, NOT_FOUND)))

      val result = testDesNpsConnector.getTaxAccount(testNino, testYear)

      await(result) mustBe None
    }
  }

  "get EmploymentData data" when {
    "given a valid Nino and TaxYear" in {

      val testemploymentsConnector = testDesNpsConnector

      when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testemploymentsConnector.http.GET[List[NpsEmployment]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(testNpsEmployment))

      val result = testemploymentsConnector.getEmployments(testNino, testYear)

      await(result) mustBe testNpsEmployment
    }

    "retrying after the first call fails and the second call succeeds" in {

      val testemploymentsConnector = testDesNpsConnector

      when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testemploymentsConnector.http.GET[List[NpsEmployment]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
        .thenReturn(Future.successful(testNpsEmployment))

      val result = testemploymentsConnector.getEmployments(testNino, testYear)

      await(result) mustBe testNpsEmployment
    }

    "return and handle an error response" in {

      val testemploymentsConnector = testDesNpsConnector
      when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testemploymentsConnector.http.GET[List[NpsEmployment]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      val result = testemploymentsConnector.getEmployments(testNino, testYear)

      intercept[BadRequestException](await(result))
    }
  }
}
