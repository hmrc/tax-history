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

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.taxhistory.metrics.MetricsEnum.MetricsEnum
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.utils.Logging

import scala.concurrent.Future

protected trait ConnectorMetrics extends Logging {
  val metrics: TaxHistoryMetrics

  protected def withMetrics[T](metric: MetricsEnum)(codeBlock: => Future[T])(implicit hc: HeaderCarrier): Future[T] = {
    val timerContext = metrics.startTimer(metric)

    codeBlock.map{ result =>
      timerContext.stop()
      metrics.incrementSuccessCounter(metric)
      result
    }.recover {
      case e : NotFoundException =>
        timerContext.stop()
        metrics.incrementSuccessCounter(metric)
        throw e
      case e =>
        metrics.incrementFailedCounter(metric)
        timerContext.stop()
        logger.warn(s"Error returned from ${getClass.getSimpleName} connector (${metric.toString}): ${e.toString}: ${e.getMessage}")
        throw e
    }
  }
}
