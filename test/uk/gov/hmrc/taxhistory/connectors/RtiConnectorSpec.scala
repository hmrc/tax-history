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
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.{GenericHttpError, InternalServerError, TaxHistoryException}
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class RtiConnectorSpec extends PlaySpec with MockitoSugar with TestUtil {
  implicit val hc = HeaderCarrier()

  val mockServicesConfig = mock[ServicesConfig]
  when(mockServicesConfig.baseUrl(any[String])).thenReturn("/test")

  val testNino = randomNino()
  val testNinoWithoutSuffix=testNino.value.take(8)
  lazy val rtiSuccessfulResponseURLDummy = loadFile("/json/rti/response/dummyRti.json")

  "RtiConnector" should {
    "have the correct RTI employments url" when {
      "given a valid nino and tax year" in {
        testRtiConnector.rtiEmploymentsUrl(testNino, TaxYear(2017)) mustBe s"/test/rti/individual/payments/nino/$testNinoWithoutSuffix/tax-year/17-18"
      }
    }

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        testRtiConnector.withoutSuffix(testNino) mustBe s"$testNinoWithoutSuffix"
      }
    }

    "create the correct headers" in {
      val headers = testRtiConnector.createHeader
      headers.extraHeaders mustBe List(("Environment", "env"), ("Authorization", "auth"), ("Gov-Uk-Originator-Id", "orgId"))
    }

    "get RTI data " when {

      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val fakeResponse: HttpResponse = HttpResponse(OK, Some(rtiSuccessfulResponseURLDummy))
        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.httpGet.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(fakeResponse))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))

        await(result) mustBe rtiSuccessfulResponseURLDummy.as[RtiData]
      }

      "return and handle an error response" in {
        val expectedResponse = Json.parse( """{"reason": "Internal Server Error"}""")
        implicit val hc = HeaderCarrier()
        when(testRtiConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testRtiConnector.httpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(expectedResponse))))

        val result = testRtiConnector.getRTIEmployments(testNino, TaxYear(2016))

        intercept[Exception](await(result)) must matchPattern {
          case TaxHistoryException(InternalServerError, _) =>
        }
      }
    }
  }

  lazy val testRtiConnector = new RtiConnector(
    httpGet = mock[HttpGet],
    httpPost = mock[HttpPost],
    audit = mock[Audit],
    servicesConfig = mockServicesConfig,
    authorizationToken = "auth",
    environment = "env",
    originatorId = "orgId"
  ) {
    override val metrics =  mock[TaxHistoryMetrics]
    val mockTimerContext = mock[Timer.Context]
  }

}


