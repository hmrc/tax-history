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

import play.api.mvc.{Action, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.TaxHistoryAuthConnector
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait EmploymentController extends AuthController {

  def employmentHistoryService: EmploymentHistoryService = EmploymentHistoryService

  def getEmployments(nino: String, taxYear: Int) = Action.async {
    implicit request => {
      authorisedRelationship(nino, _ => retrieveEmployments(nino, taxYear))
    }
  }
  def getEmployment(nino: String, taxYear: Int,employmentId:String) = Action.async {
    implicit request => {
      authorisedRelationship(nino, _ =>retrieveEmployment(nino, taxYear,employmentId))
    }
  }

  private def retrieveEmployment(nino: String,taxYear: Int,employmentId:String)(implicit hc:HeaderCarrier): Future[Result] = {
    employmentHistoryService.getEmployment(nino, taxYear,employmentId) map {
      response =>
        response.status match {
          case OK => Ok(response.body)
          case NOT_FOUND => NotFound
          case BAD_REQUEST => BadRequest(response.body)
          case SERVICE_UNAVAILABLE => ServiceUnavailable
          case _ => InternalServerError
        }
    }
  }

  private def retrieveEmployments(nino: String,taxYear: Int)(implicit hc:HeaderCarrier): Future[Result] = {
    employmentHistoryService.getEmployments(nino, taxYear) map {
      response =>
        response.status match {
          case OK => Ok(response.body)
          case NOT_FOUND => NotFound
          case BAD_REQUEST => BadRequest(response.body)
          case SERVICE_UNAVAILABLE => ServiceUnavailable
          case _ => InternalServerError
        }
    }
  }
}

object EmploymentController extends EmploymentController {
  override def authConnector: AuthConnector = TaxHistoryAuthConnector
}
