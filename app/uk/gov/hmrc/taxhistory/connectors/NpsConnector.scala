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
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger

import scala.concurrent.Future

class NpsConnector @Inject()(val http: HttpGet,
                             val metrics: TaxHistoryMetrics,
                             @Named("nps-hod-service-url") val serviceUrl: String,
                             @Named("microservice.services.nps-hod.path") val path: String,
                             @Named("microservice.services.nps-hod.originatorId") val originatorId: String) extends AnyRef with TaxHistoryLogger {

  def employmentUrl(nino: Nino, year: Int) = s"$serviceUrl/person/${nino.nino}/employment/$year"
  def iabdsUrl(nino: Nino, year: Int)      = s"$serviceUrl/person/${nino.nino}/iabds/$year"
  def taxAccountUrl(nino: Nino, year: Int) = s"$serviceUrl/person/${nino.nino}/tax-account/$year"

  def basicNpsHeaders(hc: HeaderCarrier): HeaderCarrier = {
    hc.withExtraHeaders("Gov-Uk-Originator-Id" -> originatorId)
  }

  def getEmployments(nino: Nino, year: Int): Future[List[NpsEmployment]] = {
    implicit val hc = basicNpsHeaders(HeaderCarrier())

    val timerContext = metrics.startTimer(MetricsEnum.NPS_GET_EMPLOYMENTS)

    val result = http.GET[List[NpsEmployment]](employmentUrl(nino, year))
    result.onSuccess { case _ =>
      timerContext.stop()
      metrics.incrementSuccessCounter(MetricsEnum.NPS_GET_EMPLOYMENTS)
    }
    result.onFailure { case e =>
      metrics.incrementFailedCounter(MetricsEnum.NPS_GET_EMPLOYMENTS)
      timerContext.stop()
      logger.warn(s"NPS connector - Error returned from NPS connector (getEmployments): ${e.toString}: ${e.getMessage}")
    }
    result
  }

  def getIabds(nino: Nino, year: Int): Future[List[Iabd]] = {
    implicit val hc = basicNpsHeaders(HeaderCarrier())

    val timerContext = metrics.startTimer(MetricsEnum.NPS_GET_IABDS)

    val result = http.GET[List[Iabd]](iabdsUrl(nino, year))
    result.onSuccess { case _ =>
      timerContext.stop()
      metrics.incrementSuccessCounter(MetricsEnum.NPS_GET_IABDS)
    }
    result.onFailure { case e =>
      metrics.incrementFailedCounter(MetricsEnum.NPS_GET_IABDS)
      timerContext.stop()
      logger.warn(s"NPS connector - Error returned from NPS connector (getIabds): ${e.toString}: ${e.getMessage}")
    }
    result
  }

  def getTaxAccount(nino: Nino, year: Int): Future[NpsTaxAccount] = {
    implicit val hc = basicNpsHeaders(HeaderCarrier())

    val timerContext = metrics.startTimer(MetricsEnum.NPS_GET_TAX_ACCOUNT)

    val result = http.GET[NpsTaxAccount](taxAccountUrl(nino, year))
    result.onSuccess { case _ =>
      timerContext.stop()
      metrics.incrementSuccessCounter(MetricsEnum.NPS_GET_TAX_ACCOUNT)
    }
    result.onFailure { case e =>
      metrics.incrementFailedCounter(MetricsEnum.NPS_GET_TAX_ACCOUNT)
      timerContext.stop()
      logger.warn(s"NPS connector - Error returned from NPS connector (getTaxAccount): ${e.toString}: ${e.getMessage}")
    }
    result
  }
}