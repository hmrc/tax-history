/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.utils.Retry

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesNpsConnector @Inject() (
  val http: HttpClient,
  val metrics: TaxHistoryMetrics,
  val config: AppConfig,
  val system: ActorSystem
)(implicit executionContext: ExecutionContext)
    extends ConnectorMetrics {

  private val servicePrefix = "/pay-as-you-earn"

  val withRetry: Retry                              = config.newRetryInstance("des", system)
  def iabdsUrl(nino: Nino, year: Int): String       =
    s"${config.desBaseUrl}$servicePrefix/individuals/${nino.value}/iabds/tax-year/$year"
  def taxAccountUrl(nino: Nino, year: Int): String  =
    s"${config.desBaseUrl}$servicePrefix/individuals/${nino.value}/tax-account/tax-year/$year"
  def employmentsUrl(nino: Nino, year: Int): String = s"${config.desBaseUrl}/individuals/${nino.value}/employment/$year"

  def buildHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      "Environment"      -> { config.desEnv },
      "Authorization"    -> s"Bearer ${config.desAuth}",
      CORRELATION_HEADER -> getCorrelationId(hc)
    )

  def getIabds(nino: Nino, year: Int): Future[List[Iabd]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    withMetrics(MetricsEnum.NPS_GET_IABDS) {
      withRetry {
        http.GET[List[Iabd]](iabdsUrl(nino, year), headers = buildHeaders(hc))
      }.recover {
        case UpstreamErrorResponse.Upstream4xxResponse(ex) if ex.statusCode == NOT_FOUND =>
          logger.warn(s"NPS getIabds returned a 404 response: ${ex.message}")
          List.empty
      }
    }
  }

  def getTaxAccount(nino: Nino, year: Int): Future[Option[NpsTaxAccount]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    withMetrics(MetricsEnum.NPS_GET_TAX_ACCOUNT) {
      withRetry {
        http.GET[NpsTaxAccount](taxAccountUrl(nino, year), headers = buildHeaders(hc)).map(Some(_))
      }.recover {
        case UpstreamErrorResponse.Upstream4xxResponse(ex) if ex.statusCode == NOT_FOUND =>
          logger.warn(s"NPS getTaxAccount returned a 404 response: ${ex.message}")
          None
      }
    }
  }

  def getEmployments(nino: Nino, year: Int): Future[List[NpsEmployment]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    withMetrics(MetricsEnum.NPS_GET_EMPLOYMENTS) {
      withRetry {
        http.GET[List[NpsEmployment]](employmentsUrl(nino, year), headers = buildHeaders(hc))
      }.recover {
        case UpstreamErrorResponse.Upstream4xxResponse(ex) if ex.statusCode == NOT_FOUND =>
          logger.warn(s"NPS getEmployments returned a 404 response: ${ex.message}")
          List.empty
      }
    }
  }
}
