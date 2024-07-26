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
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.utils.Retry

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsConnector @Inject() (
  val http: HttpClientV2,
  val metrics: TaxHistoryMetrics,
  val config: AppConfig,
  val system: ActorSystem
)(implicit executionContext: ExecutionContext)
    extends ConnectorMetrics {

  val withRetry: Retry = config.newRetryInstance("des", system)

  private def extractSaUtr(json: JsValue): Option[SaUtr] = (json \ "ids" \ "sautr").asOpt[SaUtr]

  /** Lookup the SA UTR for a given NINO API Spec details can be found here
    * https://github.com/hmrc/citizen-details?tab=readme-ov-file#response-status-codes
    * @param nino
    *   - NINO
    * @param hc
    *   - HeaderCarrier
    * @return
    *   - Future\[Option\[SaUtr\]\]
    */
  def lookupSaUtr(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[SaUtr]] =
    withMetrics(MetricsEnum.CITIZEN_DETAILS) {
      val fullURL = s"${config.citizenDetailsBaseUrl}/citizen-details/nino/$nino"
      withRetry {
        http
          .get(url"$fullURL")
          .execute[HttpResponse]
          .map { response =>
            response.status match {
              case 404 => None
              case 200 => extractSaUtr(response.json)
              case _   => throw UpstreamErrorResponse(response.body, response.status, response.status)
            }
          }
      }
    }

}
