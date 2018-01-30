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

package uk.gov.hmrc.taxhistory.metrics

import javax.inject.Inject

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.taxhistory.metrics.MetricsEnum.MetricsEnum

class TaxHistoryMetrics @Inject() (val metrics: Metrics) {
  val registry: MetricRegistry = metrics.defaultRegistry
  val timers = Map(
    MetricsEnum.NPS_GET_EMPLOYMENTS -> registry.timer("nps-get-employments-response-timer"),
    MetricsEnum.RTI_GET_EMPLOYMENTS -> registry.timer("rti-get-employments-response-timer"),
    MetricsEnum.NPS_GET_IABDS       -> registry.timer("nps-get-iabds-response-timer"),
    MetricsEnum.NPS_GET_TAX_ACCOUNT -> registry.timer("nps-get-tax-account-response-timer")
  )
  val successCounters = Map(
    MetricsEnum.NPS_GET_EMPLOYMENTS -> registry.counter("nps-get-employments-success-counter"),
    MetricsEnum.RTI_GET_EMPLOYMENTS -> registry.counter("rti-get-employments-success-counter"),
    MetricsEnum.NPS_GET_IABDS       -> registry.counter("nps-get-iabds-success-counter"),
    MetricsEnum.NPS_GET_TAX_ACCOUNT -> registry.counter("nps-get-tax-account-success-counter")
  )
  val failedCounters = Map(
    MetricsEnum.NPS_GET_EMPLOYMENTS -> registry.counter("nps-get-employments-failed-counter"),
    MetricsEnum.RTI_GET_EMPLOYMENTS -> registry.counter("rti-get-employments-failed-counter"),
    MetricsEnum.NPS_GET_IABDS       -> registry.counter("nps-get-iabds-failed-counter"),
    MetricsEnum.NPS_GET_TAX_ACCOUNT -> registry.counter("nps-get-tax-account-failed-counter")
  )

  def startTimer(api: MetricsEnum): Context = timers(api).time()

  def incrementSuccessCounter(api: MetricsEnum): Unit = successCounters(api).inc()

  def incrementFailedCounter(api: MetricsEnum): Unit = failedCounters(api).inc()
}