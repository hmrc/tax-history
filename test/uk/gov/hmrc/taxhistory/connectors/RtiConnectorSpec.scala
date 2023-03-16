/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.taxhistory.model.rti.RtiData
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.utils.TestUtil
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RtiConnectorSpec extends PlaySpec with MockitoSugar with TestUtil with GuiceOneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit val hc: HeaderCarrier     = HeaderCarrier()

  val mockHttpClient: HttpClient               = mock[HttpClient]
  val mockAppConfig: AppConfig                 = app.injector.instanceOf[AppConfig]
  val mockTaxHistoryMetrics: TaxHistoryMetrics = mock[TaxHistoryMetrics]
  val system: ActorSystem                      = ActorSystem("test")
  private val taxYear                          = 2016
  private val taxYearPlusOne                   = 2017

  val uuid                           = "123f4567-g89c-42c3-b456-557742330000"
  val testRtiConnector: RtiConnector = new RtiConnector(
    http = mockHttpClient,
    metrics = mockTaxHistoryMetrics,
    config = mockAppConfig,
    system = system
  ) {
    override def generateNewUUID: String = uuid
  }

  val testNino: Nino                = randomNino()
  val testNinoWithoutSuffix: String = testNino.withoutSuffix
  lazy val testRtiData: RtiData     = loadFile("/json/rti/response/dummyRti.json").as[RtiData]

  "return new ID pre-appending the requestID when the requestID matches the format(8-4-4-4)" in {
    val requestId  = "dcba0000-ij12-df34-jk56"
    val beginIndex = 24
    testRtiConnector.getCorrelationId(HeaderCarrier(requestId = Some(RequestId(requestId)))) shouldBe
      s"$requestId-${uuid.substring(beginIndex)}"
  }

  "return new ID when the requestID does not match the format(8-4-4-4)" in {
    val requestId = "1a2b-ij12-df34-jk56"
    testRtiConnector.getCorrelationId(HeaderCarrier(requestId = Some(RequestId(requestId)))) shouldBe uuid
  }

  "return the new uuid when requestID is not present in the headerCarrier" in {
    testRtiConnector.getCorrelationId(HeaderCarrier()) shouldBe uuid
  }

  "RtiConnector should have the correct RTI employments url" when {
    "given a valid nino and tax year" in {
      testRtiConnector.rtiEmploymentsUrl(
        testNino,
        TaxYear(taxYearPlusOne)
      ) mustBe s"http://localhost:9998/rti/individual/payments/nino/$testNinoWithoutSuffix/tax-year/17-18"
    }

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        testNino.withoutSuffix mustBe s"$testNinoWithoutSuffix"
      }
    }

    "create the correct headers" in {
      val headers = testRtiConnector.buildHeaders(hc)
      headers mustBe List(
        ("Environment", "local"),
        ("Authorization", "Bearer local"),
        ("CorrelationId", "123f4567-g89c-42c3-b456-557742330000")
      )
    }

    "get RTI data " when {
      "given a valid Nino and TaxYear" in {
        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.http.GET[RtiData](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(testRtiData))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(taxYear))

        await(result) mustBe Some(testRtiData)
      }

      "retrying after the first call failed and the second call succeeds" in {

        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.http.GET[RtiData](any())(any(), any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
          .thenReturn(Future.successful(testRtiData))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(taxYear))

        await(result) mustBe Some(testRtiData)
      }

      "return None when the call to RTI fails with 404 NotFound" in {

        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.http.GET[RtiData](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Not found", NOT_FOUND, NOT_FOUND)))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(taxYear))

        await(result) mustBe None
      }

      "return and handle an error response" in {

        val expectedResponse = Json.parse("""{"reason": "Internal Server Error"}""")

        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.http.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(taxYear))

        intercept[UpstreamErrorResponse](await(result) mustBe expectedResponse)
      }
    }
  }
}
