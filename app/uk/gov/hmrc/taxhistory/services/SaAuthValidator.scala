/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import uk.gov.hmrc.auth
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, Enrolment, NoActiveSession, Nino => NinoPredicate}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.taxhistory.connectors.CitizenDetailsConnector

import scala.concurrent.Future

class SaAuthService @Inject()(override val authConnector: AuthConnector,
                              override val predicateBuilder: SaAuthorisationPredicateBuilder) extends SaAuthValidator

/**
  * Checks if a logged in user is either:
  * - An individual who has a NINO that matches the requested NINO in the URL
  * - An agent with an IR-SA-AGENT enrolment and who has a delegated IR-SA enrolment for the client identified by the
  * requested NINO (where the IR-SA enrolment's UTR matches the client's SA UTR)
  */
trait SaAuthValidator extends AuthorisedFunctions with Results {

  val predicateBuilder: SaAuthorisationPredicateBuilder

  def checkSaAuthorisation(nino: Nino) = new ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

      def invokeBlockIfAuthorised(predicate: Predicate) = authorised(predicate) {
        block(request)
      } recover {
        case _: NoActiveSession => Unauthorized
        case _: AuthorisationException => Forbidden
      }

      for {
        predicate <- predicateBuilder.authorisationPredicate(nino)
        result <- invokeBlockIfAuthorised(predicate)
      } yield result
    }
  }
}

class SaAuthorisationPredicateBuilder @Inject()(val citizenDetailsConnector: CitizenDetailsConnector) {
  def authorisationPredicate(nino: Nino)(implicit hc: HeaderCarrier): Future[Predicate] = {

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
      case Some(saUtr) => checkIndividual or checkAgentServicesWithDigitalHandshake or checkAgentAuthorisationWith648(saUtr)
      case _ => checkIndividual or checkAgentServicesWithDigitalHandshake
    }
  }
}
