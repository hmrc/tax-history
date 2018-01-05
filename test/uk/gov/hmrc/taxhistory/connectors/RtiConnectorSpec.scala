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
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class RtiConnectorSpec extends PlaySpec with MockitoSugar with TestUtil {
  implicit val hc = HeaderCarrier()
  val testNino = randomNino()
  val testNinoWithoutSuffix=testNino.value.take(8)
  lazy val rtiSuccessfulResponseURLDummy = loadFile("/json/rti/response/dummyRti.json")

  "RtiConnector" should {
    "have the rti basic url " when {
      "given a valid nino" in {
        rtiConnector.rtiBasicUrl(testNino) mustBe s"/test/rti/individual/payments/nino/$testNinoWithoutSuffix"
      }
    }

    "have the Rti Path Url" when {
      "given a valid nino and path" in {
        rtiConnector.rtiPathUrl(testNino, "path") mustBe s"/test/rti/individual/payments/nino/$testNinoWithoutSuffix/path"
      }
    }

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        rtiConnector.withoutSuffix(testNino) mustBe s"$testNinoWithoutSuffix"
      }
    }

    "create the correct headers" in {
      val headers = rtiConnector.createHeader
      headers.extraHeaders mustBe List(("Environment", "env"), ("Authorization", "auth"), ("Gov-Uk-Originator-Id", "orgId"))
    }

    "get RTI data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testRtiConnector = rtiConnector
        val fakeResponse: HttpResponse = HttpResponse(OK, Some(rtiSuccessfulResponseURLDummy))
        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.httpGet.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(fakeResponse))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))
        val rtiDataResponse = await(result)

        rtiDataResponse.status mustBe OK
        rtiDataResponse.json mustBe rtiSuccessfulResponseURLDummy
      }
    }
    "return and handle a bad request response " in {
      val expectedResponse = Json.parse( """{"reason": "Some thing went wrong"}""")
      implicit val hc = HeaderCarrier()
      val testRtiConnector = rtiConnector
      when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testRtiConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(expectedResponse))))

      val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))
      val response = await(result)
      response.status must be(BAD_REQUEST)
      response.json must be(expectedResponse)
    }
    "return and handle a not found response " in {
      val expectedResponse = Json.parse( """{"reason": "Resource not found"}""")
      implicit val hc = HeaderCarrier()

      val testRtiConnector = rtiConnector
      when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testRtiConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(expectedResponse))))

      val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))
      val response = await(result)
      response.status mustBe NOT_FOUND
      response.json mustBe expectedResponse
    }
    "return and handle an internal server error response " in {
      val expectedResponse = Json.parse( """{"reason": "Internal Server Error"}""")
      implicit val hc = HeaderCarrier()
      val testRtiConnector = rtiConnector
      when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testRtiConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(expectedResponse))))

      val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))
      val response = await(result)
      response.status mustBe INTERNAL_SERVER_ERROR
      response.json mustBe expectedResponse
    }
    "return and handle an service unavailable error response " in {
      val expectedResponse = Json.parse( """{"reason": "Service Unavailable Error"}""")
      implicit val hc = HeaderCarrier()
      val testRtiConnector = rtiConnector
      when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

      when(testRtiConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(expectedResponse))))

      val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))
      val response = await(result)
      response.status mustBe SERVICE_UNAVAILABLE
      response.json mustBe expectedResponse
    }
  }

  private class TestRtiConnector extends RtiConnector {
    override val serviceUrl: String = "/test"

    override val environment: String = "env"

    override val authorization: String = "auth"

    override val originatorId: String = "orgId"

    override val httpGet: HttpGet = mock[HttpGet]

    override lazy val httpPost: HttpPost = ???
    override val metrics =  mock[TaxHistoryMetrics]

    val mockTimerContext = mock[Timer.Context]
  }
  private def rtiConnector = new TestRtiConnector

}


