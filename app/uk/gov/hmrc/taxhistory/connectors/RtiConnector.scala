/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.utils.Retry
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RtiConnector @Inject()(val http: HttpClient,
                             val metrics: TaxHistoryMetrics,
                             val config: AppConfig,
                             val system: ActorSystem)
                             (implicit executionContext: ExecutionContext) extends ConnectorMetrics {

  lazy val authorization: String = s"Bearer ${config.desAuth}"
  val withRetry: Retry = config.newRetryInstance("des", system)

  def rtiEmploymentsUrl(nino: Nino, taxYear: TaxYear): String = {
    val formattedTaxYear = s"${taxYear.startYear % 100}-${taxYear.finishYear % 100}"
    s"${config.desBaseUrl}/rti/individual/payments/nino/${nino.withoutSuffix}/tax-year/$formattedTaxYear"
  }

  def createHeader: HeaderCarrier = HeaderCarrier(extraHeaders =
    Seq("Environment" -> config.desEnv,
      "Authorization" -> authorization))

  def getRTIEmployments(nino: Nino, taxYear: TaxYear): Future[Option[RtiData]] = {
    implicit val hc: HeaderCarrier = createHeader

    withMetrics(MetricsEnum.RTI_GET_EMPLOYMENTS) {
      withRetry {
        http.GET[RtiData](rtiEmploymentsUrl(nino, taxYear))
          .map(Some(_))
      }.recover {
        case _: NotFoundException => None
      }

    }
  }
}