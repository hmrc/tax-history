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

import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.taxhistory.metrics.MetricsEnum.MetricsEnum
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import play.api.Logging

import scala.concurrent.{ExecutionContext, Future}

protected trait ConnectorMetrics extends ConnectorCorrelationId with Logging {
  val metrics: TaxHistoryMetrics

  protected def withMetrics[T](
    metric: MetricsEnum
  )(codeBlock: => Future[T])(implicit executionContext: ExecutionContext): Future[T] = {
    val timerContext = metrics.startTimer(metric)

    codeBlock
      .map { result =>
        timerContext.stop()
        metrics.incrementSuccessCounter(metric)
        result
      }
      .recover {
        case e: NotFoundException =>
          timerContext.stop()
          metrics.incrementSuccessCounter(metric)
          logger.warn(
            s"[ConnectorMetrics][withMetrics] NotFound Exception ${getClass.getSimpleName} connector (${metric.toString}) : ${e.getMessage}"
          )
          throw e
        case e                    =>
          metrics.incrementFailedCounter(metric)
          timerContext.stop()
          logger.error(
            s"[ConnectorMetrics][withMetrics] Error returned from ${getClass.getSimpleName} connector (${metric.toString}) : ${e.getMessage}"
          )
          throw e
      }
  }
}
