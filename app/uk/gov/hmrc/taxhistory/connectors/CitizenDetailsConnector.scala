/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.utils.Retry

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsConnector @Inject()(val http: HttpClient,
                                        val metrics: TaxHistoryMetrics,
                                        val config: AppConfig,
                                        val system: ActorSystem)
                                       (implicit executionContext: ExecutionContext)  extends ConnectorMetrics {

  val withRetry: Retry = config.newRetryInstance("des", system)

  def lookupSaUtr(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[SaUtr]] = {
    withMetrics(MetricsEnum.CITIZEN_DETAILS) {
      withRetry {
        http.GET[JsValue](s"${config.citizenDetailsBaseUrl}/citizen-details/nino/$nino").map { json =>
          (json \ "ids" \ "sautr").asOpt[SaUtr]
        }
      }
    }.recover {
      case _: NotFoundException => None
    }
  }
}