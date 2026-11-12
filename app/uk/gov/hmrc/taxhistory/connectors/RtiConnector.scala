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
import sttp.model.HeaderNames
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.model.rti.RtiData
import uk.gov.hmrc.taxhistory.utils.Retry
import uk.gov.hmrc.time.TaxYear

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RtiConnector @Inject() (
  val http: HttpClientV2,
  val metrics: TaxHistoryMetrics,
  val config: AppConfig,
  val system: ActorSystem,
  val util: HeaderHelper
)(implicit executionContext: ExecutionContext)
    extends ConnectorMetrics {

  val withRetry: Retry = config.newRetryInstance("nps.hip", system)

  def rtiEmploymentsUrl(nino: Nino, taxYear: TaxYear): String = {
    val formattedTaxYear = s"${taxYear.startYear % 100}-${taxYear.finishYear % 100}"
    val endpoint         = s"/rti/individual/payments/nino/${nino.withoutSuffix}/tax-year/$formattedTaxYear"
    if (config.api5990) s"${config.hipBaseUrl}$endpoint" else s"${config.rtiDesBaseUrl}$endpoint"
  }

  def getRTIEmployments(nino: Nino, taxYear: TaxYear): Future[Option[RtiData]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    withMetrics(MetricsEnum.RTI_GET_EMPLOYMENTS) {
      withRetry {
        val fullURL = rtiEmploymentsUrl(nino, taxYear)
        http
          .get(url"$fullURL")
          .setHeader(util.createHeaders(hc): _*)
          .execute[HttpResponse]
          .map { response =>
            response.status match {
              case 404 =>
                logger.warn(
                  s"RTIEmployments returned a 404 response: ${response.body}"
                )
                None
              case 200 =>
                Some(response.json.as[RtiData])
              case _   => throw UpstreamErrorResponse(response.body, response.status, response.status)
            }
          }
      }
    }
  }
}
