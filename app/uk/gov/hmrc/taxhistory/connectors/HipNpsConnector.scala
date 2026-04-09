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

import org.apache.pekko.actor.ActorSystem
import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.model.nps._
import uk.gov.hmrc.taxhistory.utils.Retry

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HipNpsConnector @Inject() (
  val http: HttpClientV2,
  val metrics: TaxHistoryMetrics,
  val config: AppConfig,
  val system: ActorSystem
)(implicit executionContext: ExecutionContext)
    extends ConnectorMetrics {

  val withRetry: Retry = config.newRetryInstance("nps.hip", system)

  def employmentsUrl(nino: Nino, year: Int): String =
    s"${config.hipBaseUrl}/paye/employment/employee/${nino.value}/tax-year/$year/employment-details"

  def taxAccountUrl(nino: Nino, taxYear: Int): String =
    s"${config.hipBaseUrl}/paye/person/${nino.value}/tax-account/$taxYear"

  def iabdsUrl(nino: Nino, year: Int): String =
    s"${config.hipBaseUrl}/paye/iabd/taxpayer/$nino/tax-year/$year"

  def buildHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      config.serviceOriginatorIdKey -> config.serviceOriginatorIDValue,
      HIP_CORRELATION_HEADER        -> getHIPCorrelationId(hc),
      HIP_AUTHORIZATION_HEADER      -> s"Basic ${config.authorizationToken}"
    )

  def getIabds(nino: Nino, year: Int): Future[List[Iabd]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    withMetrics(MetricsEnum.NPS_GET_IABDS) {
      withRetry {
        val fullURL = iabdsUrl(nino, year)
        http
          .get(url"$fullURL")
          .setHeader(buildHeaders: _*)
          .execute[HttpResponse]
          .map { response =>
            response.status match {
              case NOT_FOUND =>
                logger.warn(s"[DesNpsConnector][getIabds] NPS getIabds returned a 404 response: ${response.body}")
                List.empty
              case OK        => response.json.as[IabdList].getListOfIabd
              case _         => throw UpstreamErrorResponse(response.body, response.status, response.status)
            }
          }
      }
    }
  }

  def getTaxAccount(nino: Nino, year: Int): Future[Option[NpsTaxAccount]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    withMetrics(MetricsEnum.NPS_GET_TAX_ACCOUNT) {
      withRetry {
        val fullURL = taxAccountUrl(nino, year)
        http
          .get(url"$fullURL")
          .setHeader(buildHeaders: _*)
          .execute[HttpResponse]
          .map { response =>
            response.status match {
              case NOT_FOUND =>
                logger.warn(
                  s"[DesNpsConnector][getTaxAccount] NPS getTaxAccount returned a 404 response: ${response.body}"
                )
                None
              case OK        => response.json.asOpt[NpsTaxAccount]
              case _         => throw UpstreamErrorResponse(response.body, response.status, response.status)
            }
          }
      }
    }
  }

  def getEmployments(nino: Nino, year: Int): Future[List[NpsEmployment]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    withMetrics(MetricsEnum.NPS_GET_EMPLOYMENTS) {
      withRetry {
        val fullURL = employmentsUrl(nino, year)
        http
          .get(url"$fullURL")
          .setHeader(buildHeaders: _*)
          .execute[HttpResponse]
          .map { response =>
            response.status match {
              case NOT_FOUND                                  =>
                logger.warn(
                  s"[DesNpsConnector][getEmployments] NPS getEmployments returned a 404 response: ${response.body}"
                )
                List.empty
              case OK if response.body.equalsIgnoreCase("{}") => List.empty
              case OK                                         =>
                response.json.as[NpsEmployments].toListOfNpsEmployment
              case _                                          =>
                throw UpstreamErrorResponse(
                  response.body,
                  response.status,
                  response.status
                )
            }
          }
      }
    }
  }
}
