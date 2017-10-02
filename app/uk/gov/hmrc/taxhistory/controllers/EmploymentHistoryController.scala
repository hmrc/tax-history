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

import play.api.Logger
import play.api.mvc.Action
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.taxhistory.TaxHistoryAuthConnector
import uk.gov.hmrc.taxhistory.model.auth.AfiAuth._
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


trait EmploymentHistoryController extends BaseController with AuthorisedFunctions {

  def employmentHistoryService: EmploymentHistoryService = EmploymentHistoryService

  private def isAgent(group: AffinityGroup): Boolean = group.toString.contains("Agent")

  private def extractArn(enrolls: Set[Enrolment]): Option[String] = {
    Logger.info("enrolls=" + enrolls)

    def extractArn(enrollmentIndenitifiers: Seq[EnrolmentIdentifier]) = enrollmentIndenitifiers.find(_.key == "AgentReferenceNumber").map(_.value)

    val arn: Option[String] = enrolls.find(_.key equals "HMRC-AS-AGENT").flatMap(agentEnrollment ⇒ extractArn(agentEnrollment.identifiers))
    Logger.info("arn=" + arn)
    arn
  }

  def getEmploymentHistory(nino: String, taxYear: Int) = Action.async {
    implicit request =>
      authorised(AgentEnrolmentForPAYE.withIdentifier("MTDITID", nino) and AuthProviderAgents and ConfidenceLevel.L200).retrieve(affinityGroupAllEnrolls) {

        case Some(affinityG) ~ allEnrols ⇒

          Logger.info("allEnrols " + allEnrols)
          Logger.info("affinityGroup " + affinityG)
          (isAgent(affinityG), extractArn(allEnrols.enrolments)) match {
            case (true, Some(arn)) => getTaxHistory(nino, taxYear)
            case _ =>Future.successful(Unauthorized("Not Authorised"))
          }
        case _ => {
          Logger.debug("failed to retrieve")
          Future.successful(Unauthorized("Not Authorised"))
        }
      }.recoverWith {
        case i: InsufficientEnrolments =>
          Logger.error("Error thrown :" + i.getMessage)
          Future.successful(Unauthorized("Not Authorised"))
        case e =>
          Logger.error("Error thrown :" + e.getMessage)
          Future.successful(InternalServerError(e.getMessage))
      }

  }

  private def getTaxHistory(nino: String, taxYear: Int)(implicit hc:HeaderCarrier) = {
    employmentHistoryService.getEmploymentHistory(nino, taxYear) map {
      response =>
        response.status match {
          case OK => Ok(response.body)
          case NOT_FOUND => NotFound(response.body)
          case BAD_REQUEST => BadRequest(response.body)
          case SERVICE_UNAVAILABLE => ServiceUnavailable(response.body)
          case _ => InternalServerError(response.body)
        }
    }
  }
}

object EmploymentHistoryController extends EmploymentHistoryController {
  override def authConnector: AuthConnector = TaxHistoryAuthConnector
}
