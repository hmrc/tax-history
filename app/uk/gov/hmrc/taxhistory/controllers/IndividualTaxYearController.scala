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

package uk.gov.hmrc.taxhistory.controllers

import play.api.mvc.{Action, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.TaxHistoryAuthConnector
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait IndividualTaxYearController extends AuthController {

  def employmentHistoryService: EmploymentHistoryService = EmploymentHistoryService

  def getTaxYears(nino: String) = Action.async {
    implicit request => {
      authorisedRelationship(nino, retrieveTaxYears(nino))
    }
  }

  private def retrieveTaxYears(nino: String)(implicit hc:HeaderCarrier): Future[Result] = {
    employmentHistoryService.getTaxYears(nino = nino) map {
      response =>
        response.status match {
          case OK => Ok(response.body)
          case _ => InternalServerError
        }
    }
  }
}

object IndividualTaxYearController extends IndividualTaxYearController {
  override def authConnector: AuthConnector = TaxHistoryAuthConnector
}

