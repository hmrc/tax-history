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

package uk.gov.hmrc.taxhistory.controllers

import javax.inject.Inject

import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.audit.{AgentViewedClient, AgentViewedClientEvent, DataEventDetail}
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger

import scala.concurrent.Future

class IndividualTaxYearController @Inject()(val authConnector: AuthConnector,
                                            val employmentHistoryService: EmploymentHistoryService,
                                            val auditable: Auditable) extends TaxHistoryController with TaxHistoryLogger {

  def getTaxYears(nino: String): Action[AnyContent] = Action.async {
    implicit request => {
      authorisedRelationship(nino, data => retrieveTaxYears(nino, data.getOrElse("")))
    }
  }

  private def retrieveTaxYears(nino: String, arn: String)(implicit hc: HeaderCarrier): Future[Result] =
    employmentHistoryService.getTaxYears(nino = nino) map {
      response =>
        response.status match {
          case OK =>
            auditable.sendDataEvent(transactionName = AgentViewedClient,
              path = "/tax-history/select-tax-year",
              detail = DataEventDetail(Map("agentReferenceNumber" -> arn, "nino" -> nino)),
              eventType = AgentViewedClientEvent)
            Ok(response.body)
          case _ =>
            logger.warn(s"Internal Server Error ${response.body}")
            InternalServerError

        }
    }
}