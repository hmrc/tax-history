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
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment, RtiPayment}
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

import play.api.test.Helpers._


class RtiConnectorSpec extends PlaySpec with MockitoSugar with TestUtil {

  lazy val rtiSuccessfulResponseURLDummy = loadFile("/json/rti/response/dummyRti.json")

  "RtiConnector" should {
    "have the rti basic url " when {
      "given a valid nino" in {
        createSUT.rtiBasicUrl(Nino("AA111111A")) mustBe "/test/rti/individual/payments/nino/AA111111"
      }
    }

    "have the Rti Path Url" when {
      "given a valid nino and path" in {
        createSUT.rtiPathUrl(Nino("AA111111A"), "path") mustBe "/test/rti/individual/payments/nino/AA111111/path"
      }
    }

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        createSUT.withoutSuffix(Nino("AA111111A")) mustBe "AA111111"
      }
    }

    "have createHeader" in {
      val headers = createSUT.createHeader
      headers.extraHeaders mustBe List(("Environment", "env"), ("Authorization", "auth"), ("Gov-Uk-Originator-Id", "orgId"))
    }

    "have get RTI" when {
      "given a valid Nino and TaxYear" in {
        val sut = createSUT
        implicit val hc = HeaderCarrier()

        val fakeResponse: HttpResponse = HttpResponse(200, Some(rtiSuccessfulResponseURLDummy))

        when(sut.httpGet.GET[HttpResponse](any())(any(), any())).thenReturn(Future.successful(fakeResponse))

        val result = sut.getRTI(Nino("AA000000A"), 16)
        val rtiData = await(result)

        rtiData mustBe Some(rtiSuccessfulResponseURLDummy.as[RtiData](RtiData.reader))

      }
    }
  }

  private class SUT extends RtiConnector {
    override val serviceUrl: String = "/test"

    override val environment: String = "env"

    override val authorization: String = "auth"

    override val originatorId: String = "orgId"

    override val httpGet: HttpGet = mock[HttpGet]

    override lazy val httpPost: HttpPost = ???

    val mockTimerContext = mock[Timer.Context]
  }
  private def createSUT = new SUT

}


