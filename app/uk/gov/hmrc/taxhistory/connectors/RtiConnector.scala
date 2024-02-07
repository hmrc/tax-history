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

import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.taxhistory.model.rti.RtiData
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.utils.Retry
import uk.gov.hmrc.time.TaxYear
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RtiConnector @Inject() (
  val http: HttpClient,
  val metrics: TaxHistoryMetrics,
  val config: AppConfig,
  val system: ActorSystem
)(implicit executionContext: ExecutionContext)
    extends ConnectorMetrics {

  val withRetry: Retry = config.newRetryInstance("des", system)

  def rtiEmploymentsUrl(nino: Nino, taxYear: TaxYear): String = {
    val formattedTaxYear = s"${taxYear.startYear % 100}-${taxYear.finishYear % 100}"
    s"${config.desBaseUrl}/rti/individual/payments/nino/${nino.withoutSuffix}/tax-year/$formattedTaxYear"
  }

  def buildHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      "Environment"      -> { config.desEnv },
      "Authorization"    -> s"Bearer ${config.desAuth}",
      CORRELATION_HEADER -> getCorrelationId(hc)
    )

  def getRTIEmployments(nino: Nino, taxYear: TaxYear): Future[Option[RtiData]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    withMetrics(MetricsEnum.RTI_GET_EMPLOYMENTS) {
      withRetry {
        http
          .GET[RtiData](rtiEmploymentsUrl(nino, taxYear), headers = buildHeaders(hc))
          .map(Some(_))
      }.recover {
        case UpstreamErrorResponse.Upstream4xxResponse(ex) if ex.statusCode == 404 =>
          logger.warn(s"RTIEmployments returned a 404 response: ${ex.message}")
          None
      }
    }
  }

}
