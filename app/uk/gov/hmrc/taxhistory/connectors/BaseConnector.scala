/*
 * Copyright 2017 HM Revenue & Customs
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

import play.Logger
import play.api.http.Status
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.taxhistory.MicroserviceAuditConnector
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.metrics.{MetricsEnum, TaxHistoryMetrics}

import scala.concurrent.Future

trait BaseConnector extends ServicesConfig with Auditable{

  def httpGet: CoreGet
  def httpPost: CorePost
  def originatorId: String
  def metrics: TaxHistoryMetrics = TaxHistoryMetrics

  val defaultVersion: Int = -1

  implicit val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }

  def basicNpsHeaders(hc: HeaderCarrier): HeaderCarrier = {
    hc.withExtraHeaders("Gov-Uk-Originator-Id" -> originatorId)
  }

  def withoutSuffix(nino: Nino) = {
    val BASIC_NINO_LENGTH = 8
    nino.value.take(BASIC_NINO_LENGTH)
  }

  override def appName: String = AppName.appName

  override def audit: Audit = new Audit(AppName.appName, MicroserviceAuditConnector)

  def getFromRTI(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val timerContext = metrics.startTimer(MetricsEnum.RTI_GET_EMPLOYMENTS)

    val futureResponse = httpGet.GET[HttpResponse](url)
    futureResponse.flatMap {
      timerContext.stop()
      res =>
        res.status match {
          case Status.OK => {
            metrics.incrementSuccessCounter(MetricsEnum.RTI_GET_EMPLOYMENTS)
            Future.successful(res)
          }
          case Status.BAD_REQUEST => {
            metrics.incrementFailedCounter(MetricsEnum.RTI_GET_EMPLOYMENTS)
            val errorMessage = s"RTIAPI - Bad Request error returned from RTI HODS"
            Logger.warn(errorMessage)
            Future.successful(res)
          }
          case Status.NOT_FOUND => {
            metrics.incrementFailedCounter(MetricsEnum.RTI_GET_EMPLOYMENTS)
            val errorMessage =s"RTIAPI - No DATA Found error returned from RTI HODS"
            Logger.warn(errorMessage)
            Future.successful(res)
          }
          case Status.INTERNAL_SERVER_ERROR => {
            metrics.incrementFailedCounter(MetricsEnum.RTI_GET_EMPLOYMENTS)
            val errorMessage =s"RTIAPI - Internal Server error returned from RTI HODS"
            Logger.warn(errorMessage)
            Future.successful(res)
          }
          case status => {
            metrics.incrementFailedCounter(MetricsEnum.RTI_GET_EMPLOYMENTS)
            val errorMessage =s"RTIAPI - An error returned from RTI HODS with status $status"
            Logger.warn(errorMessage)
            Future.successful(res)
          }
        }
    }
  }

 }
