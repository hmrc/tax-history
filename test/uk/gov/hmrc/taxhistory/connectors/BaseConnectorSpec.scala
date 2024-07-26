/*
 * Copyright 2024 HM Revenue & Customs
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
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.utils.TestUtil

import scala.concurrent.Future

trait BaseConnectorSpec extends PlaySpec with TestUtil with BeforeAndAfterEach with GuiceOneAppPerSuite {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()

  val system: ActorSystem = ActorSystem("test")

  val mockHttpClient: HttpClientV2       = mock(classOf[HttpClientV2])
  val mockRequestBuilder: RequestBuilder = mock(classOf[RequestBuilder])
  val mockMetrics: TaxHistoryMetrics     = mock(classOf[TaxHistoryMetrics])
  val mockTimerContext: Timer.Context    = mock(classOf[Timer.Context])
  val mockAppConfig: AppConfig           = app.injector.instanceOf[AppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockHttpClient)
    reset(mockMetrics)
    reset(mockTimerContext)
    reset(mockRequestBuilder)

    when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)

    when(mockMetrics.startTimer(any())).thenReturn(mockTimerContext)

  }

  def buildHttpResponse(body: String, status: Int = OK): HttpResponse =
    HttpResponse(status, body = body, Map.empty[String, Seq[String]])

  def buildHttpResponse(status: Int): HttpResponse =
    HttpResponse(status, body = "", Map.empty[String, Seq[String]])

  def mockExecuteMethod(body: String, status: Int): Unit =
    when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
      .thenReturn(Future.successful(buildHttpResponse(body, status)))

  def mockExecuteMethod(status: Int): Unit =
    when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
      .thenReturn(Future.successful(buildHttpResponse("", status)))

}
