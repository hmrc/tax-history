/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.utils.Retry
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class RtiConnector @Inject()(val http: HttpClient,
                             val metrics: TaxHistoryMetrics,
                             @Named("des-base-url") val baseUrl: String,
                             @Named("microservice.services.des.authorizationToken") val authorizationToken: String,
                             @Named("microservice.services.des.env") val environment: String, @Named("des") val withRetry: Retry
                            ) extends ConnectorMetrics {

  lazy val authorization: String = s"Bearer $authorizationToken"

  def rtiEmploymentsUrl(nino: Nino, taxYear: TaxYear): String = {
    val formattedTaxYear = s"${taxYear.startYear % 100}-${taxYear.finishYear % 100}"
    s"$baseUrl/rti/individual/payments/nino/${nino.withoutSuffix}/tax-year/$formattedTaxYear"
  }

  def createHeader: HeaderCarrier = HeaderCarrier(extraHeaders =
    Seq("Environment" -> environment,
      "Authorization" -> authorization))

  def getRTIEmployments(nino: Nino, taxYear: TaxYear): Future[RtiData] = {
    implicit val hc: HeaderCarrier = createHeader

    withMetrics(MetricsEnum.RTI_GET_EMPLOYMENTS) {
      withRetry {
        http.GET[RtiData](rtiEmploymentsUrl(nino, taxYear))
      }
    }
  }
}