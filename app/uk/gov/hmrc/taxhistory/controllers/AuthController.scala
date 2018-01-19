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

import play.api.mvc.Result
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger

import scala.concurrent.Future

trait AuthController extends BaseController with AuthorisedFunctions with TaxHistoryLogger{

  lazy val affinityGroupAllEnrolls: Retrieval[~[Option[AffinityGroup], Enrolments]] = affinityGroup and allEnrolments

  lazy val AgentEnrolmentForPAYE: Enrolment = Enrolment("HMRC-AS-AGENT")
    .withDelegatedAuthRule("afi-auth")

  def extractArn(enrolls: Enrolments): Option[String] = for {
    agentEnrol <- enrolls.getEnrolment("HMRC-AS-AGENT")
    arn <- agentEnrol.getIdentifier("AgentReferenceNumber")
  } yield arn.value

  def authorisedRelationship(nino: String, action: (Option[String]) => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authorised(AgentEnrolmentForPAYE.withIdentifier("MTDITID", nino) and AuthProviders(GovernmentGateway)).retrieve(affinityGroupAllEnrolls) {
      case affinityG ~ allEnrols =>
        (affinityG, extractArn(allEnrols)) match {
          case (Some(Agent), Some(arn)) => action(Some(arn))
          case _ => Future.successful(Unauthorized("Not Authorised"))
        }
      case _ =>
        logger.debug("Failed to retrieve affinity group or enrolments")
        Future.successful(Unauthorized("Not Authorised"))
    }.recoverWith {
      case i: InsufficientEnrolments =>
        logger.error("InsufficientEnrolments: " + i.getMessage)
        Future.successful(Unauthorized("Not Authorised"))
      case e =>
        logger.error("Error thrown :" + e.getMessage)
        Future.successful(InternalServerError(e.getMessage))
    }
  }
}