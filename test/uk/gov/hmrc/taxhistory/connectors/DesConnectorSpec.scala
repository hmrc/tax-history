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
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, DesTaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import scala.concurrent.Future

class DesConnectorSpec extends PlaySpec with MockitoSugar with TestUtil {

  lazy val testIabds = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]
  lazy val testDesTaxAccount = loadFile("/json/nps/response/GetTaxAccount.json").as[DesTaxAccount]

  val testNino = randomNino()
  val testYear = 2016

  "EmploymentConnector" should {

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        testNino.withoutSuffix mustBe s"${testNino.value.take(8)}"
      }
    }

    "create the correct headers" in {
      val headers = testDesConnector.basicDesHeaders(HeaderCarrier())
      headers.extraHeaders mustBe List(("Environment", "test"), ("Authorization", "someToken"))
    }

    "create the correct url for iabds" in {
      testDesConnector.iabdsUrl(testNino, testYear) must be(s"/fake/pay-as-you-earn/individuals/$testNino/iabds/tax-year/$testYear")
    }

    "create the correct url for taxAccount" in {
      testDesConnector.taxAccountUrl(testNino, testYear) must be(s"/fake/pay-as-you-earn/individuals/$testNino/tax-account/tax-year/$testYear")
    }

    "get Iabds data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()

        when(testDesConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testDesConnector.http.GET[List[Iabd]](any())(any(), any(), any())).thenReturn(Future.successful(testIabds))

        val result = testDesConnector.getIabds(testNino, testYear)

        await(result) mustBe testIabds
      }

      "return and handle an service unavailable error response " in {
        implicit val hc = HeaderCarrier()
        when(testDesConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testDesConnector.http.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.failed(new Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

        val result = testDesConnector.getIabds(testNino, testYear)

        intercept[Upstream5xxResponse](await(result))
      }
    }

    "get Tax Account data " when {
      "given a valid Nino and TaxYear" in {
        implicit val hc = HeaderCarrier()
        val testTaxAccountConnector = testDesConnector

        when(testTaxAccountConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testTaxAccountConnector.http.GET[DesTaxAccount](any())(any(), any(), any())).thenReturn(Future.successful(testDesTaxAccount))

        val result = testTaxAccountConnector.getTaxAccount(testNino, testYear)

        await(result) mustBe testDesTaxAccount
      }

      "return and handle an error response" in {
        implicit val hc = HeaderCarrier()
        when(testDesConnector.metrics.startTimer(any())).thenReturn(new Timer().time())

        when(testDesConnector.http.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(Future.failed(new BadRequestException("")))

        val result = testDesConnector.getTaxAccount(testNino, testYear)

        intercept[BadRequestException](await(result))
      }
    }

  }

  lazy val testDesConnector = new DesConnector(
    http = mock[HttpGet],
    baseUrl = "/fake",
    metrics = mock[TaxHistoryMetrics],
    authorizationToken = "someToken",
    env = "test"
  ) {
    override val metrics = mock[TaxHistoryMetrics]
    val mockTimerContext = mock[Timer.Context]
  }
}
