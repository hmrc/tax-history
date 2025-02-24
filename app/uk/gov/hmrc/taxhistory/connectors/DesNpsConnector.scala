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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.model.nps.HIPNpsEmployments.toListOfHIPNpsEmployment
import uk.gov.hmrc.taxhistory.model.nps.{HIPNpsEmployment, HIPNpsEmployments, HipErrors, Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.utils.Retry

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesNpsConnector @Inject() (
  val http: HttpClientV2,
  val metrics: TaxHistoryMetrics,
  val config: AppConfig,
  val system: ActorSystem
)(implicit executionContext: ExecutionContext)
    extends ConnectorMetrics {

  private val servicePrefix = "/pay-as-you-earn"

  val withRetry: Retry = config.newRetryInstance("des", system)

  def iabdsUrl(nino: Nino, year: Int): String =
    s"${config.desBaseUrl}$servicePrefix/individuals/${nino.value}/iabds/tax-year/$year"

  def taxAccountUrl(nino: Nino, year: Int): String  =
    s"${config.desBaseUrl}$servicePrefix/individuals/${nino.value}/tax-account/tax-year/$year"
//TODO: remove employmentsUrl
  def employmentsUrl(nino: Nino, year: Int): String = s"${config.desBaseUrl}/individuals/${nino.value}/employment/$year"

  def employmentsHIPUrl(nino: Nino, year: Int): String =
    s"${config.hipBaseUrl}/employment/employee/${nino.value}/tax-year/$year/employment-details"

  def buildHIPHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      config.hipServiceOriginatorIdKey -> config.hipServiceOriginatorId,
      HIP_CORRELATION_HEADER           -> getCorrelationId(hc),
      HIP_AUTHORIZATION_HEADER         -> s"Basic ${config.authorizationToken}"
    )

  def buildHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      "Environment"      -> config.desEnv,
      "Authorization"    -> s"Bearer ${config.desAuth}",
      CORRELATION_HEADER -> getCorrelationId(hc)
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
              case 404 =>
                logger.warn(s"[DesNpsConnector][getIabds] NPS getIabds returned a 404 response: ${response.body}")
                List.empty
              case 200 => response.json.as[List[Iabd]]
              case _   => throw UpstreamErrorResponse(response.body, response.status, response.status)
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
              case 404 =>
                logger.warn(
                  s"[DesNpsConnector][getTaxAccount] NPS getTaxAccount returned a 404 response: ${response.body}"
                )
                None
              case 200 => response.json.asOpt[NpsTaxAccount]
              case _   => throw UpstreamErrorResponse(response.body, response.status, response.status)
            }
          }
      }
    }
  }

  def getEmployments(nino: Nino, year: Int): Future[List[NpsEmployment]] =
    if (config.isUsingHIP) {
      //to be changed
      implicit val hc: HeaderCarrier = HeaderCarrier()
      withMetrics(MetricsEnum.NPS_GET_EMPLOYMENTS) {
        withRetry {
          val fullURL = employmentsHIPUrl(nino, year)
          http
            .get(url"$fullURL")
            .setHeader(buildHIPHeaders: _*)
            .execute[HttpResponse]
            .map { response =>
              response.status match {
                case 404                                         =>
                  logger.warn(
                    s"[DesNpsConnector][getEmployments] NPS getEmployments returned a 404 response: ${response.body}"
                  )
                  List.empty
                case 200 if response.body.equalsIgnoreCase("{}") => List.empty
                case 200                                         =>
                  toListOfHIPNpsEmployment(response.json.as[HIPNpsEmployments])
                    .map[NpsEmployment](HIPNpsEmployment.toNpsEmployment)
                //TODO:Remove .map as it maps to des NpsEmployment
                case _                                           =>
                  throw UpstreamErrorResponse(
                    response.json.validate[HipErrors].toString,
                    response.status,
                    response.status
                  )
              }
            }
        }
      }
    } else {
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
                case 404 =>
                  logger.warn(
                    s"[DesNpsConnector][getEmployments] NPS getEmployments returned a 404 response"
                  )
                  List.empty
                case 200 => response.json.as[List[NpsEmployment]]
                case _   => throw UpstreamErrorResponse(response.body, response.status, response.status)
              }
            }
        }
      }
    }
}
