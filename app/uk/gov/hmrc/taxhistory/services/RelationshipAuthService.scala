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

package uk.gov.hmrc.taxhistory.services

import javax.inject.Inject
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.taxhistory.utils.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  * This service provides a method to check whether an authorised relationship exists between a NINO and an Agent
  * before proceeding with a given code block.
  */
class RelationshipAuthService @Inject() (val authConnector: AuthConnector)(implicit executionContext: ExecutionContext)
    extends AuthorisedFunctions
    with Results
    with Logging {

  lazy val affinityGroupAllEnrolls: Retrieval[Option[AffinityGroup] ~ Enrolments] = affinityGroup and allEnrolments

  lazy val AgentEnrolmentForPAYE: Enrolment = Enrolment("HMRC-AS-AGENT")
    .withDelegatedAuthRule("afi-auth")

  private def extractArn(enrolls: Enrolments): Option[Arn] = for {
    agentEnrol <- enrolls.getEnrolment("HMRC-AS-AGENT")
    arn        <- agentEnrol.getIdentifier("AgentReferenceNumber")
  } yield Arn(arn.value)

  def retrieveArnFor(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[Arn]] =
    authorised(AgentEnrolmentForPAYE.withIdentifier("MTDITID", nino.value) and AuthProviders(GovernmentGateway))
      .retrieve(affinityGroupAllEnrolls) {
        case affinityG ~ allEnrols =>
          (affinityG, extractArn(allEnrols)) match {
            case (Some(Agent), Some(arn)) => Future.successful(Some(arn))
            case _                        => Future.successful(None)
          }
        case _                     =>
          logger.debug("Failed to retrieve affinity group or enrolments")
          Future.failed(new UnauthorizedException("Failed to retrieve affinity group or enrolments"))
      }

  /**
    * A code block wrapped in this function will only be executed if there exists an authorised relationship
    * between the given NINO and an Agent.
    */
  def withAuthorisedRelationship(
    nino: Nino
  )(action: Arn => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    retrieveArnFor(nino)
      .flatMap {
        case Some(arn) => action(arn)
        case None      =>
          logger.info(s"No ARN found for $nino")
          Future.successful(Unauthorized)
      }
      .recoverWith {
        case e: UnauthorizedException  =>
          logger.info("Unauthorized: " + e.getMessage)
          Future.successful(Unauthorized(e.getMessage))
        case e: InsufficientEnrolments =>
          logger.info("InsufficientEnrolments: " + e.getMessage)
          Future.successful(Unauthorized(e.getMessage))
        case e                         =>
          logger.info("Error thrown :" + e.getMessage)
          Future.successful(InternalServerError(e.getMessage))
      }

}
