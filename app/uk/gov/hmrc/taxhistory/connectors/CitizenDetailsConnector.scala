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

package uk.gov.hmrc.taxhistory.connectors

import javax.inject.{Inject, Named}
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}

import scala.concurrent.Future

class CitizenDetailsConnector @Inject()(val http: HttpGet,
                                        val metrics: TaxHistoryMetrics,
                                        @Named("citizen-details-base-url") val baseUrl: String) extends ConnectorMetrics {
  def lookupSaUtr(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[SaUtr]] = {
    withMetrics(MetricsEnum.CITIZEN_DETAILS) {
      http.GET[JsValue](s"$baseUrl/citizen-details/nino/$nino").map { json =>
        (json \ "ids" \ "sautr").asOpt[SaUtr]
      }
    }.recover {
      case _: NotFoundException => None
    }
  }
}