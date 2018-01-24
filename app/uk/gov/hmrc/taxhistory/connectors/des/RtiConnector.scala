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

package uk.gov.hmrc.taxhistory.connectors.des

import javax.inject.{Inject, Named}

import play.api.http.Status
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.taxhistory.connectors.BaseConnector
import uk.gov.hmrc.taxhistory.metrics.MetricsEnum
import uk.gov.hmrc.time.TaxYear

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class RtiConnector @Inject()(val httpGet: CoreGet,
                             val httpPost: CorePost,
                             val audit: Audit,
                             val servicesConfig: ServicesConfig,
                             @Named("microservice.services.rti-hod.authorizationToken") val authorizationToken: String,
                             @Named("microservice.services.rti-hod.env") val environment: String,
                             @Named("microservice.services.rti-hod.originatorId") val originatorId: String
                            ) extends BaseConnector {

  lazy val serviceUrl: String = s"${servicesConfig.baseUrl("rti-hod")}"

  lazy val authorization: String = s"Bearer $authorizationToken"

  def rtiEmploymentsUrl(nino: Nino, taxYear: TaxYear): String = {
    val formattedTaxYear = s"${taxYear.startYear % 100}-${taxYear.finishYear % 100}"
    s"$serviceUrl/rti/individual/payments/nino/${withoutSuffix(nino)}/tax-year/$formattedTaxYear"
  }

  def createHeader: HeaderCarrier = HeaderCarrier(extraHeaders =
    Seq("Environment" -> environment,
      "Authorization" -> authorizationToken,
      "Gov-Uk-Originator-Id" -> originatorId))

  def getRTIEmployments(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeader
    getFromRTI(rtiEmploymentsUrl(nino, taxYear))
  }

  def getFromRTI(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val timerContext = metrics.startTimer(MetricsEnum.RTI_GET_EMPLOYMENTS)

    val futureResponse = httpGet.GET[HttpResponse](url)
    futureResponse.flatMap {
      timerContext.stop()
      res =>
        res.status match {
          case Status.OK =>
            metrics.incrementSuccessCounter(MetricsEnum.RTI_GET_EMPLOYMENTS)
            Future.successful(res)
          case Status.BAD_REQUEST =>
            metrics.incrementFailedCounter(MetricsEnum.RTI_GET_EMPLOYMENTS)
            val errorMessage = s"RTIAPI - Bad Request error returned from RTI HODS"
            logger.warn(errorMessage)
            Future.successful(res)
          case Status.NOT_FOUND =>
            metrics.incrementFailedCounter(MetricsEnum.RTI_GET_EMPLOYMENTS)
            val errorMessage = s"RTIAPI - No DATA Found error returned from RTI HODS"
            logger.warn(errorMessage)
            Future.successful(res)
          case Status.INTERNAL_SERVER_ERROR =>
            metrics.incrementFailedCounter(MetricsEnum.RTI_GET_EMPLOYMENTS)
            val errorMessage = s"RTIAPI - Internal Server error returned from RTI HODS"
            logger.warn(errorMessage)
            Future.successful(res)
          case status =>
            metrics.incrementFailedCounter(MetricsEnum.RTI_GET_EMPLOYMENTS)
            val errorMessage = s"RTIAPI - An error returned from RTI HODS with status $status"
            logger.warn(errorMessage)
            Future.successful(res)
        }
    }
  }
}