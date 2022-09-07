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
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.concurrent.Eventually._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.utils.TestUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CitizenDetailsConnectorSpec
    extends PlaySpec
    with MockitoSugar
    with TestUtil
    with BeforeAndAfterEach
    with GuiceOneAppPerSuite {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()

  private val mockHttp         = mock[HttpClient]
  private val mockMetrics      = mock[TaxHistoryMetrics]
  private val mockTimerContext = mock[Timer.Context]
  private val mockAppConfig    = app.injector.instanceOf[AppConfig]
  private val system           = ActorSystem("test")

  private val testConnector =
    new CitizenDetailsConnector(http = mockHttp, metrics = mockMetrics, config = mockAppConfig, system = system)

  override def beforeEach: Unit = {
    reset(mockHttp)
    reset(mockMetrics)
    reset(mockTimerContext)
  }

  "lookupSaUtr" should {

    "return UTR if citizen-details has an 'sautr' for a given NINO" in
      new CitizenDetailsRespondsWithUtr(forThisNino = Nino("AA000003D")) {
        await(testConnector.lookupSaUtr(forThisNino)) mustBe Some(expectedUtr)
      }

    "return None if citizen-details does not have an 'sautr' for a given NINO" in
      new CitizenDetailsRespondsWithoutUtr(forThisNino = Nino("AA000003D")) {
        await(testConnector.lookupSaUtr(Nino("AA000003D"))) mustBe None
      }

    "return None if citizen-details returns 404 for a given NINO" in
      new CitizenDetailsFails(new NotFoundException("")) {
        await(testConnector.lookupSaUtr(Nino("AA000003D"))) mustBe None
      }

    "return UpstreamErrorResponse if citizen-details returns 5xx" in
      new CitizenDetailsFails(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)) {
        intercept[UpstreamErrorResponse](await(testConnector.lookupSaUtr(Nino("AA000003D"))))
      }

    "will attempt a retry upon failure" in
      new CitizenDetailsFailsOnceThenRespondsWithUtr(
        UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR),
        forThisNino = Nino("AA000003D")
      ) {
        await(testConnector.lookupSaUtr(forThisNino)) mustBe Some(expectedUtr)
      }

    "record metrics" when {
      "increment successful metric counter on successful call that returned a UTR" in
        new CitizenDetailsRespondsWithUtr(forThisNino = Nino("AA000003D")) {
          await(testConnector.lookupSaUtr(forThisNino))
          eventually(verify(mockMetrics).incrementSuccessCounter(MetricsEnum.CITIZEN_DETAILS))
        }

      "increment successful metric counter on successful call that returned no UTR" in
        new CitizenDetailsRespondsWithoutUtr(forThisNino = Nino("AA000003D")) {
          await(testConnector.lookupSaUtr(forThisNino))
          eventually(verify(mockMetrics).incrementSuccessCounter(MetricsEnum.CITIZEN_DETAILS))
        }

      "increment successful metric counter on call that returned 404" in
        new CitizenDetailsFails(new NotFoundException("")) {
          await(testConnector.lookupSaUtr(Nino("AA000003D")))
          eventually(verify(mockMetrics).incrementSuccessCounter(MetricsEnum.CITIZEN_DETAILS))
        }

      "tracks time of successful calls" in
        new CitizenDetailsRespondsWithUtr(forThisNino = Nino("AA000003D")) {
          await(testConnector.lookupSaUtr(forThisNino))
          verify(mockMetrics).startTimer(MetricsEnum.CITIZEN_DETAILS)
          eventually(verify(mockTimerContext).stop())
        }

      "increment failed metric counter on failed call" in
        new CitizenDetailsFails(new BadRequestException("")) {
          intercept[Throwable](await(testConnector.lookupSaUtr(Nino("AA000003D"))))
          eventually(verify(mockMetrics).incrementFailedCounter(MetricsEnum.CITIZEN_DETAILS))
        }

      "tracks time of unsuccessful calls" in
        new CitizenDetailsFails(new BadRequestException("")) {
          intercept[Throwable](await(testConnector.lookupSaUtr(Nino("AA000003D"))))
          verify(mockMetrics).startTimer(MetricsEnum.CITIZEN_DETAILS)
          eventually(verify(mockTimerContext).stop())
        }
    }
  }

  class CitizenDetailsRespondsWithUtr(val forThisNino: Nino) {
    val expectedUtr: SaUtr = SaUtr("1097133333")
    when(mockMetrics.startTimer(any())).thenReturn(mockTimerContext)
    when(mockHttp.GET[JsValue](any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(responseWithUtr(forThisNino, expectedUtr)))
  }

  class CitizenDetailsRespondsWithoutUtr(val forThisNino: Nino) {
    when(mockMetrics.startTimer(any())).thenReturn(mockTimerContext)
    when(mockHttp.GET[JsValue](any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Json.parse(s"""
           |{
           |    "dateOfBirth": "23041948",
           |    "ids": {
           |        "nino": "${forThisNino.value}"
           |    },
           |    "name": {
           |        "current": {
           |            "firstName": "Jim",
           |            "lastName": "Ferguson"
           |        },
           |        "previous": []
           |    }
           |}
        """.stripMargin)))
  }

  class CitizenDetailsFails(withThisException: Throwable) {
    when(mockMetrics.startTimer(any())).thenReturn(mockTimerContext)
    when(mockHttp.GET[JsValue](any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.failed(withThisException))
  }

  class CitizenDetailsFailsOnceThenRespondsWithUtr(withThisException: Throwable, val forThisNino: Nino) {
    val expectedUtr: SaUtr = SaUtr("1097133333")
    when(mockMetrics.startTimer(any())).thenReturn(mockTimerContext)
    when(mockHttp.GET[JsValue](any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.failed(withThisException))
      .thenReturn(Future.successful(responseWithUtr(forThisNino, expectedUtr)))
  }

  private def responseWithUtr(forThisNino: Nino, expectedUtr: SaUtr) =
    Json.parse(s"""
         |{
         |    "dateOfBirth": "23041948",
         |    "ids": {
         |        "nino": "${forThisNino.value}",
         |        "sautr": "${expectedUtr.value}"
         |    },
         |    "name": {
         |        "current": {
         |            "firstName": "Jim",
         |            "lastName": "Ferguson"
         |        },
         |        "previous": []
         |    }
         |}
        """.stripMargin)
}
