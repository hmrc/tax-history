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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.taxhistory.services.{EmploymentHistoryService, RelationshipAuthService}
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class AllowanceController @Inject()(val employmentHistoryService: EmploymentHistoryService,
                                    val relationshipAuthService: RelationshipAuthService) extends TaxHistoryController {

  def getAllowances(nino: String, taxYear: Int): Action[AnyContent] = Action.async { implicit request =>
    relationshipAuthService.withAuthorisedRelationship(Nino(nino)) { _ =>
      retrieveAllowances(nino, taxYear)
    }
  }

  private def retrieveAllowances(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[Result] = toResult {
    employmentHistoryService.getAllowances(Nino(nino), TaxYear(taxYear))
  }
}