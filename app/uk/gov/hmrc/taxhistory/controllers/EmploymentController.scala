/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.taxhistory.services.{EmploymentHistoryService, RelationshipAuthService}
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmploymentController @Inject() (
  val employmentHistoryService: EmploymentHistoryService,
  val relationshipAuthService: RelationshipAuthService,
  val cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends TaxHistoryController(cc) {

  def getEmployments(nino: String, taxYear: Int): Action[AnyContent] = Action.async { implicit request =>
    relationshipAuthService.withAuthorisedRelationship(Nino(nino)) { _ =>
      retrieveEmployments(Nino(nino), TaxYear(taxYear))
    }
  }

  def getEmployment(nino: String, taxYear: Int, employmentId: String): Action[AnyContent] = Action.async {
    implicit request =>
      relationshipAuthService.withAuthorisedRelationship(Nino(nino)) { _ =>
        retrieveEmployment(Nino(nino), TaxYear(taxYear), employmentId)
      }
  }

  private def retrieveEmployment(nino: Nino, taxYear: TaxYear, employmentId: String)(implicit
    hc: HeaderCarrier
  ): Future[Result] = toResult {
    employmentHistoryService.getEmployment(nino, taxYear, employmentId)
  }

  private def retrieveEmployments(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Result] = toResult {
    employmentHistoryService.getEmployments(nino, taxYear)
  }
}
