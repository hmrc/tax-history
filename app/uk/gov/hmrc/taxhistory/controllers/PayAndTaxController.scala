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

import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.taxhistory.TaxHistoryAuthConnector
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService

import scala.concurrent.Future

class PayAndTaxController(val authConnector: AuthConnector, val employmentHistoryService: EmploymentHistoryService) extends TaxHistoryController {

  def getPayAndTax(nino: String, taxYear: Int, employmentId: String): Action[AnyContent] = Action.async {
    implicit request => {
      authorisedRelationship(nino, _ => retrievePayAndTax(nino, taxYear, employmentId))
    }
  }

  private def retrievePayAndTax(nino: String,taxYear: Int, employmentId: String)(implicit hc:HeaderCarrier): Future[Result] =
    employmentHistoryService.getPayAndTax(nino = nino, taxYear = taxYear, employmentId = employmentId) map matchResponse
}

object ProdPayAndTaxController extends PayAndTaxController(new TaxHistoryAuthConnector(), EmploymentHistoryService)
