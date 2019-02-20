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
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.utils.Retry
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration


class RtiConnectorSpec extends PlaySpec with MockitoSugar with TestUtil {
  implicit val hc = HeaderCarrier()

  val testNino = randomNino()
  val testNinoWithoutSuffix = testNino.withoutSuffix
  lazy val testRtiData = loadFile("/json/rti/response/dummyRti.json").as[RtiData]

  "RtiConnector" should {
    "have the correct RTI employments url" when {
      "given a valid nino and tax year" in {
        testRtiConnector.rtiEmploymentsUrl(testNino, TaxYear(2017)) mustBe s"/test/rti/individual/payments/nino/$testNinoWithoutSuffix/tax-year/17-18"
      }
    }

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        testNino.withoutSuffix mustBe s"$testNinoWithoutSuffix"
      }
    }

    "create the correct headers" in {
      val headers = testRtiConnector.createHeader
      headers.extraHeaders mustBe List(("Environment", "env"), ("Authorization", "Bearer auth"))
    }

    "get RTI data " when {

      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.http.GET[RtiData](any())(any(), any(), any())).thenReturn(Future.successful(testRtiData))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))

        await(result) mustBe testRtiData
      }

      "return and handle an error response" in {
        val expectedResponse = Json.parse( """{"reason": "Internal Server Error"}""")
        implicit val hc = HeaderCarrier()
        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.http.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.failed(new Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))

        intercept[Upstream5xxResponse](await(result))
      }
    }
  }

  private val system = ActorSystem("test")
  private val delay = FiniteDuration(1000, TimeUnit.MILLISECONDS)

  lazy val testRtiConnector = new RtiConnector(
    http = mock[HttpClient],
    baseUrl = "/test",
    metrics = mock[TaxHistoryMetrics],
    authorizationToken = "auth",
    environment = "env", withRetry = new Retry(1, delay, system)
  ) {
    override val metrics =  mock[TaxHistoryMetrics]
    val mockTimerContext = mock[Timer.Context]
  }
}
