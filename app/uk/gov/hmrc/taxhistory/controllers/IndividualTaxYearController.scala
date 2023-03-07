/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.Arn
import uk.gov.hmrc.taxhistory.model.audit.{AgentViewedClient, AgentViewedClientEvent, DataEventDetail}
import uk.gov.hmrc.taxhistory.services.{EmploymentHistoryService, RelationshipAuthService}
import uk.gov.hmrc.taxhistory.utils.Logging

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IndividualTaxYearController @Inject() (
  val employmentHistoryService: EmploymentHistoryService,
  val relationshipAuthService: RelationshipAuthService,
  val auditable: Auditable,
  val cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends TaxHistoryController(cc)
    with Logging {

  def getTaxYears(nino: String): Action[AnyContent] = Action.async { implicit request =>
    relationshipAuthService.withAuthorisedRelationship(Nino(nino)) { arn =>
      retrieveTaxYears(Nino(nino), arn)
    }
  }

  private def retrieveTaxYears(nino: Nino, arn: Arn)(implicit hc: HeaderCarrier): Future[Result] = {
    val taxYears = employmentHistoryService.getTaxYears(nino)
    taxYears.onComplete { _ =>
      auditable.sendDataEvent(
        transactionName = AgentViewedClient,
        path = "/tax-history/select-tax-year",
        detail = DataEventDetail(Map("agentReferenceNumber" -> arn.value, "nino" -> nino.value)),
        eventType = AgentViewedClientEvent
      )
    }
    toResult(taxYears)
  }
}
