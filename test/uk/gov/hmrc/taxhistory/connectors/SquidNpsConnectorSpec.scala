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
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.utils.Retry

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class SquidNpsConnectorSpec extends PlaySpec with MockitoSugar with TestUtil {

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
      val headers = testNpsConnector.basicNpsHeaders(HeaderCarrier())
      headers.extraHeaders mustBe List(("Gov-Uk-Originator-Id", "orgId"))
    }

    "create the correct url for employment" in {
      testNpsConnector.employmentUrl(testNino, testYear) must be (s"/fake/nps-hod-service/services/nps/person/$testNino/employment/$testYear")
    }

    "get EmploymentData data" when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testemploymentsConnector = testNpsConnector

        when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testemploymentsConnector.http.GET[List[NpsEmployment]](any())(any(), any(), any())).thenReturn(Future.successful(testNpsEmployment))

        val result = testemploymentsConnector.getEmployments(testNino, testYear)

        await(result) mustBe testNpsEmployment
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
  }

  private val system = ActorSystem("test")
  private val delay = FiniteDuration(1000, TimeUnit.MILLISECONDS)

  lazy val testNpsConnector = new SquidNpsConnector(
    http = mock[HttpClient],
    baseUrl = "/fake",
    metrics = mock[TaxHistoryMetrics],
    originatorId = "orgId", withRetry = new Retry(1, delay, system)
  ) {
    override val metrics = mock[TaxHistoryMetrics]
    val mockTimerContext = mock[Timer.Context]
  }
}
