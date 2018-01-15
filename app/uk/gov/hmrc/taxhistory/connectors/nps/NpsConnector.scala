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

package uk.gov.hmrc.taxhistory.connectors.nps

import play.api.Logger
import play.api.http.Status.OK
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.taxhistory.WSHttp
import uk.gov.hmrc.taxhistory.connectors.BaseConnector
import uk.gov.hmrc.taxhistory.metrics.MetricsEnum

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import scala.concurrent.Future

trait NpsConnector extends BaseConnector {

  def serviceUrl: String

  def npsBaseUrl(nino: Nino) = s"$serviceUrl/person/$nino"

  def npsPathUrl(nino: Nino, path: String) = s"${npsBaseUrl(nino)}/$path"

  def getEmployments(nino: Nino, year: Int)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val hc = basicNpsHeaders(HeaderCarrier())
    val urlToRead = npsPathUrl(nino, s"employment/$year")

    val timerContext = metrics.startTimer(MetricsEnum.NPS_GET_EMPLOYMENTS)

    httpGet.GET[HttpResponse](urlToRead).map { response =>
      timerContext.stop()
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.NPS_GET_EMPLOYMENTS)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.NPS_GET_EMPLOYMENTS)
          Logger.warn(s"[NpsConnector][getEmploymentsOLD] - status: $status Error ${response.body}")
          response
      }
    }
  }

  def getIabds(nino: Nino, year: Int)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val hc = basicNpsHeaders(HeaderCarrier())
    val urlToRead = npsPathUrl(nino, s"iabds/$year")

    val timerContext = metrics.startTimer(MetricsEnum.NPS_GET_IABDS)

    httpGet.GET[HttpResponse](urlToRead).map { response =>
      timerContext.stop()
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.NPS_GET_IABDS)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.NPS_GET_IABDS)
          Logger.warn(s"[NpsConnector][getIabds] - status: $status Error ${response.body}")
          response
      }
    }
  }

  def getTaxAccount(nino: Nino, year: Int)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val hc = basicNpsHeaders(HeaderCarrier())
    val urlToRead = npsPathUrl(nino, s"tax-account/$year")

    val timerContext = metrics.startTimer(MetricsEnum.NPS_GET_TAX_ACCOUNT)

    httpGet.GET[HttpResponse](urlToRead).map { response =>
      timerContext.stop()
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.NPS_GET_TAX_ACCOUNT)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.NPS_GET_TAX_ACCOUNT)
          Logger.warn(s"[NpsConnector][getTaxAccount] - status: $status Error ${response.body}")
          response
      }
    }
  }

}

object NpsConnector extends NpsConnector {
  // $COVERAGE-OFF$
  override val httpGet: CoreGet = WSHttp
  override val httpPost: CorePost = WSHttp
  lazy val path: String = config("nps-hod").getString("path").fold("")(p => p)
  lazy val serviceUrl: String = s"${baseUrl("nps-hod")}$path"
  lazy val originatorId: String = getConfString("nps-hod.originatorId", "HMRC_GDS")
  // $COVERAGE-ON$
}