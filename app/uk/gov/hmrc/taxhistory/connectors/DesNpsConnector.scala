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

import com.codahale.metrics.Timer.Context
import javax.inject.{Inject, Named}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.taxhistory.metrics.MetricsEnum.MetricsEnum
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.utils.Logging

import scala.concurrent.{ExecutionContext, Future}

class DesNpsConnector @Inject()(val http: HttpGet,
                             val metrics: TaxHistoryMetrics,
                             @Named("des-base-url") val baseUrl: String,
                             @Named("microservice.services.des.authorizationToken") val authorizationToken: String,
                             @Named("microservice.services.des.env") val env: String) extends AnyRef with Logging {

  private val servicePrefix = "/pay-as-you-earn"
  def iabdsUrl(nino: Nino, year: Int)      = s"$baseUrl$servicePrefix/individuals/${nino.value}/iabds/tax-year/$year"
  def taxAccountUrl(nino: Nino, year: Int) = s"$baseUrl$servicePrefix/individuals/${nino.value}/tax-account/tax-year/$year"

  def basicDesHeaders(hc: HeaderCarrier): HeaderCarrier = {
    hc.withExtraHeaders("Environment" -> env,
      "Authorization" -> s"Bearer $authorizationToken")
  }

  def getIabds(nino: Nino, year: Int): Future[List[Iabd]] = {
    implicit val hc = basicDesHeaders(HeaderCarrier())

    withMetrics(MetricsEnum.NPS_GET_IABDS) {
      http.GET[List[Iabd]](iabdsUrl(nino, year))
    }
  }

  def getTaxAccount(nino: Nino, year: Int): Future[NpsTaxAccount] = {
    implicit val hc = basicDesHeaders(HeaderCarrier())

    withMetrics(MetricsEnum.NPS_GET_TAX_ACCOUNT) {
      http.GET[NpsTaxAccount](taxAccountUrl(nino, year))
    }
  }

  private def withMetrics[T](metric: MetricsEnum)(codeBlock: => Future[T])(implicit hc: HeaderCarrier) = {
    val timerContext = metrics.startTimer(metric)

    val result: Future[T] = codeBlock

    result.onSuccess{
      case _ =>
        timerContext.stop()
        metrics.incrementSuccessCounter(metric)
    }

    result.onFailure{
      case _ : NotFoundException  =>
        timerContext.stop()
        metrics.incrementSuccessCounter(metric)
      case e =>
        metrics.incrementFailedCounter(metric)
        timerContext.stop()
        logger.warn(s"DES NPS connector - Error returned from DES NPS connector (${metric.toString}): ${e.toString}: ${e.getMessage}")
    }

    result
  }
}