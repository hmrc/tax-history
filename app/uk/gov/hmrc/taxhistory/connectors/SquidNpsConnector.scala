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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.taxhistory.metrics.MetricsEnum.MetricsEnum
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.utils.Logging

import scala.concurrent.Future

class SquidNpsConnector @Inject()(val http: HttpGet,
                                  val metrics: TaxHistoryMetrics,
                                  @Named("nps-hod-base-url") val baseUrl: String,
                                  @Named("microservice.services.nps-hod.originatorId") val originatorId: String) extends AnyRef with Logging {

  private val servicePrefix = "nps-hod-service/services/nps"
  def employmentUrl(nino: Nino, year: Int) = s"$baseUrl/$servicePrefix/person/${nino.value}/employment/$year"

  def basicNpsHeaders(hc: HeaderCarrier): HeaderCarrier = {
    hc.withExtraHeaders("Gov-Uk-Originator-Id" -> originatorId)
  }

  def getEmployments(nino: Nino, year: Int): Future[List[NpsEmployment]] = {
    implicit val hc = basicNpsHeaders(HeaderCarrier())

    withMetrics(MetricsEnum.NPS_GET_EMPLOYMENTS) {
      http.GET[List[NpsEmployment]](employmentUrl(nino, year))
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
        logger.warn(s"SQUID NPS connector - Error returned from SQUID NPS connector (${metric.toString}): ${e.toString}: ${e.getMessage}")
    }

    result
  }
}