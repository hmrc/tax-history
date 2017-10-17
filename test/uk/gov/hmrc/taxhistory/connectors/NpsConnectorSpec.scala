/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

import scala.concurrent.Future


class NpsConnectorSpec extends PlaySpec with MockitoSugar with TestUtil {

  lazy val employmentsSuccessfulResponse = loadFile("/json/nps/response/employments.json")
  lazy val iabdsSuccessfulResponse = loadFile("/json/nps/response/iabds.json")

  val testNino = randomNino()
  
  "EmploymentsConnector" should {
    "have the nps basic url " when {
      "given a valid nino" in {
        npsConnector.npsBaseUrl(testNino) mustBe s"/test/person/$testNino"
      }
    }

    "have the nps Path Url" when {
      "given a valid nino and path" in {
        npsConnector.npsPathUrl(testNino, "path") mustBe s"/test/person/$testNino/path"
      }
    }

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        npsConnector.withoutSuffix(testNino) mustBe s"${testNino.value.take(8)}"
      }
    }

    "create the correct headers" in {
      val headers = npsConnector.basicNpsHeaders(HeaderCarrier())
      headers.extraHeaders mustBe List(("Gov-Uk-Originator-Id", "orgId"))
    }

    "get EmploymentData data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testemploymentsConnector = npsConnector
        val fakeResponse: HttpResponse = HttpResponse(OK, Some(employmentsSuccessfulResponse))

        when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testemploymentsConnector.httpGet.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(fakeResponse))

        val result = testemploymentsConnector.getEmployments(testNino, 2016)
        val rtiDataResponse = await(result)

        rtiDataResponse.status mustBe OK
        rtiDataResponse.json mustBe employmentsSuccessfulResponse
      }
    }


    "return and handle a bad request response " in {
      val expectedResponse = Json.parse( """{"reason": "Some thing went wrong"}""")
      implicit val hc = HeaderCarrier()
      val testemploymentsConnector = npsConnector
      when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testemploymentsConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(expectedResponse))))

      val result = testemploymentsConnector.getEmployments(testNino, 2016)
      val response = await(result)
      response.status must be(BAD_REQUEST)
      response.json must be(expectedResponse)
    }
    "return and handle a not found response " in {
      val expectedResponse = Json.parse( """{"reason": "Resource not found"}""")
      implicit val hc = HeaderCarrier()
      val testemploymentsConnector = npsConnector
      when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testemploymentsConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(expectedResponse))))

      val result = testemploymentsConnector.getEmployments(testNino, 2016)
      val response = await(result)
      response.status mustBe NOT_FOUND
      response.json mustBe expectedResponse
    }
    "return and handle an internal server error response " in {
      val expectedResponse = Json.parse( """{"reason": "Internal Server Error"}""")
      implicit val hc = HeaderCarrier()
      val testemploymentsConnector = npsConnector
      when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testemploymentsConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(expectedResponse))))

      val result = testemploymentsConnector.getEmployments(testNino, 2016)
      val response = await(result)
      response.status mustBe INTERNAL_SERVER_ERROR
      response.json mustBe expectedResponse
    }
    "return and handle an service unavailable error response " in {
      val expectedResponse = Json.parse( """{"reason": "Service Unavailable Error"}""")
      implicit val hc = HeaderCarrier()
      val testemploymentsConnector = npsConnector
      when(testemploymentsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testemploymentsConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(expectedResponse))))

      val result = testemploymentsConnector.getEmployments(testNino, 2016)
      val response = await(result)
      response.status mustBe SERVICE_UNAVAILABLE
      response.json mustBe expectedResponse
    }

    "get Iabds data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testIabdsConnector = npsConnector
        val fakeResponse: HttpResponse = HttpResponse(OK, Some(iabdsSuccessfulResponse))

        when(testIabdsConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testIabdsConnector.httpGet.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(fakeResponse))

        val result = testIabdsConnector.getIabds(testNino, 2016)
        val rtiDataResponse = await(result)

        rtiDataResponse.status mustBe OK
        rtiDataResponse.json mustBe iabdsSuccessfulResponse
      }
    }

  }

  private class TestNpsConnector extends NpsConnector {
    override val serviceUrl: String = "/test"

    override val metrics = mock[TaxHistoryMetrics]

    override val originatorId: String = "orgId"

    override val httpGet: HttpGet = mock[HttpGet]

    override lazy val httpPost: HttpPost = ???

    val mockTimerContext = mock[Timer.Context]
  }
  private def npsConnector = new TestNpsConnector

}


