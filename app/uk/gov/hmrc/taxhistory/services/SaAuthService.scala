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

package uk.gov.hmrc.taxhistory.services

import javax.inject.Inject
import play.api.mvc.{AnyContent, Request, Result, Results}
import uk.gov.hmrc.auth
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, Enrolment, NoActiveSession}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.connectors.CitizenDetailsConnector
import uk.gov.hmrc.taxhistory.utils.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  * Checks if a logged in user is either:
  * - An individual who has a NINO that matches the requested NINO in the URL
  * - An agent with an IR-SA-AGENT enrolment and who has a delegated IR-SA enrolment for the client identified by the
  * requested NINO (where the IR-SA enrolment's UTR matches the client's SA UTR)
  */
class SaAuthService @Inject() (val authConnector: AuthConnector, val citizenDetailsConnector: CitizenDetailsConnector)(
  implicit val ec: ExecutionContext
) extends AuthorisedFunctions
    with Results
    with Logging {

  def authorisationPredicate(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Predicate] = {

    val checkIndividual: Predicate = auth.core.Nino(hasNino = true, nino = Some(nino.value))

    val checkAgentServicesWithDigitalHandshake: Predicate =
      Enrolment("THIS-STRING-IS-NOT-RELEVANT")
        .withIdentifier("NINO", nino.value)
        .withDelegatedAuthRule("afi-auth")

    def checkAgentAuthorisationWith648(saUtr: SaUtr): Predicate =
      Enrolment("IR-SA")
        .withIdentifier("UTR", saUtr.value)
        .withDelegatedAuthRule("sa-auth")

    citizenDetailsConnector.lookupSaUtr(nino).map {
      case Some(saUtr) =>
        checkIndividual or checkAgentServicesWithDigitalHandshake or checkAgentAuthorisationWith648(saUtr)
      case _           => checkIndividual or checkAgentServicesWithDigitalHandshake
    }
  }

  /**
    * A code block wrapped in this function will only be executed if the logged in NINO matches the request NINO or
    * there exists an authorised relationship between the given NINO and an Agent.
    */
  def withSaAuthorisation(nino: Nino)(
    action: Request[AnyContent] => Future[Result]
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    authorisationPredicate(nino).flatMap { pred =>
      authorised(pred) {
        action(request)
      } recover {
        case _: NoActiveSession        => Unauthorized
        case _: AuthorisationException => Forbidden
      }
    }

}
