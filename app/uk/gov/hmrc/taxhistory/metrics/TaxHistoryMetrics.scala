package uk.gov.hmrc.taxhistory.metrics

import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import uk.gov.hmrc.play.graphite.MicroserviceMetrics
import uk.gov.hmrc.taxhistory.metrics.MetricsEnum.MetricsEnum

trait TaxHistoryMetrics {

  def startTimer(api: MetricsEnum): Timer.Context

  def incrementSuccessCounter(api: MetricsEnum): Unit

  def incrementFailedCounter(api: MetricsEnum): Unit

}


object TaxHistoryMetrics extends TaxHistoryMetrics with MicroserviceMetrics {
  val registry = metrics.defaultRegistry
  val timers = Map(
    MetricsEnum.NPS_GET_EMPLOYMENTS -> registry.timer("nps-get-employments-response-timer"),
    MetricsEnum.RTI_GET_EMPLOYMENTS -> registry.timer("rti-get-employments-response-timer")

  )
  val successCounters = Map(
    MetricsEnum.NPS_GET_EMPLOYMENTS -> registry.counter("nps-get-employments-success-counter"),
    MetricsEnum.RTI_GET_EMPLOYMENTS -> registry.counter("rti-get-employments-success-counter")

  )
  val failedCounters = Map(
    MetricsEnum.NPS_GET_EMPLOYMENTS -> registry.counter("nps-get-employments-failed-counter"),
    MetricsEnum.RTI_GET_EMPLOYMENTS -> registry.counter("rti-get-employments-failed-counter")

  )

  override def startTimer(api: MetricsEnum): Context = timers(api).time()

  override def incrementSuccessCounter(api: MetricsEnum): Unit = successCounters(api).inc()

  override def incrementFailedCounter(api: MetricsEnum): Unit = failedCounters(api).inc()

}