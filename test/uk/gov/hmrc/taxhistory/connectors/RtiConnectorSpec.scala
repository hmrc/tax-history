/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class RtiConnectorSpec extends PlaySpec with MockitoSugar with TestUtil with GuiceOneAppPerSuite  {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  val mockHttpClient: HttpClient = mock[HttpClient]
  val mockAppConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val mockTaxHistoryMetrics: TaxHistoryMetrics = mock[TaxHistoryMetrics]
  val system: ActorSystem = ActorSystem("test")

  val testRtiConnector: RtiConnector = new RtiConnector(
    http = mockHttpClient,
    metrics = mockTaxHistoryMetrics,
    config = mockAppConfig,
    system = system
  )

  val testNino: Nino = randomNino()
  val testNinoWithoutSuffix: String = testNino.withoutSuffix
  lazy val testRtiData: RtiData = loadFile("/json/rti/response/dummyRti.json").as[RtiData]

  "RtiConnector" should {
    "have the correct RTI employments url" when {
      "given a valid nino and tax year" in {
        testRtiConnector.rtiEmploymentsUrl(testNino, TaxYear(2017)) mustBe s"http://localhost:9998/rti/individual/payments/nino/$testNinoWithoutSuffix/tax-year/17-18"
      }
    }

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        testNino.withoutSuffix mustBe s"$testNinoWithoutSuffix"
      }
    }

    "create the correct headers" in {
      val headers = testRtiConnector.createHeader
      headers.extraHeaders mustBe List(("Environment", "local"), ("Authorization", "Bearer local"))
    }

    "get RTI data " when {

      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.http.GET[RtiData](any())(any(), any(), any()))
          .thenReturn(Future.successful(testRtiData))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))

        await(result) mustBe Some(testRtiData)
      }

      "retrying after the first call failed and the second call succeeds" in {
        implicit val hc = HeaderCarrier()
        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.http.GET[RtiData](any())(any(), any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
          .thenReturn(Future.successful(testRtiData))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))

        await(result) mustBe Some(testRtiData)
      }

      "return None when the call to RTI fails with 404 NotFound" in {
        implicit val hc = HeaderCarrier()
        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.http.GET[RtiData](any())(any(), any(), any()))
          .thenReturn(Future.failed(new NotFoundException("")))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))

        await(result) mustBe None
      }

      "return and handle an error response" in {
        val expectedResponse = Json.parse( """{"reason": "Internal Server Error"}""")
        implicit val hc = HeaderCarrier()
        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.http.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))

        intercept[UpstreamErrorResponse](await(result) mustBe expectedResponse)
      }
    }
  }
}
