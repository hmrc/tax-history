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

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, BearerTokenExpired, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.connectors.CitizenDetailsConnector

import scala.concurrent.Future

class SaAuthServiceSpec extends PlaySpec with MockitoSugar {

  val validNino = Nino("AA000000A")

  "SaAuthorisationPredicateBuilder" should {

    val foundSaUtr = SaUtr("UtrFoundForThatNino")

    trait Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]
      val authPredicateBuilder = new SaAuthorisationPredicateBuilder(mockCitizenDetailsConnector)

      val selfAssessmentForAgents648Predicate =
        Enrolment("IR-SA")
          .withIdentifier("UTR", foundSaUtr.value)
          .withDelegatedAuthRule("sa-auth")

      val individualPredicate = auth.core.Nino(hasNino = true, nino = Some(validNino.value))

      val agentServicesAccountWithDigitalHandshakePredicate =
        Enrolment("THIS-STRING-IS-NOT-RELEVANT")
          .withIdentifier("NINO", validNino.value)
          .withDelegatedAuthRule("afi-auth")
    }

    "use individual authorisation predicate if no SA UTR is found for the NINO" in new Setup {

      when(mockCitizenDetailsConnector.lookupSaUtr(meq(validNino))(any()))
        .thenReturn(Future.successful(None))

      val result = await(authPredicateBuilder.authorisationPredicate(validNino)(hc))

      result mustBe (individualPredicate or agentServicesAccountWithDigitalHandshakePredicate)
    }

    "use individual or agent authorisation predicate if an SA UTR is found for the NINO" in new Setup {

      when(mockCitizenDetailsConnector.lookupSaUtr(meq(validNino))(any()))
        .thenReturn(Future.successful(Some(foundSaUtr)))

      val result = await(authPredicateBuilder.authorisationPredicate(validNino)(hc))

      result mustBe (individualPredicate or agentServicesAccountWithDigitalHandshakePredicate or selfAssessmentForAgents648Predicate)
    }
  }

  "SaAuthService" should {
    trait Setup {
      val mockAuthConnector = mock[AuthConnector]
      val mockSaAuthPredicateBuilder = mock[SaAuthorisationPredicateBuilder]
      val mockPredicate = mock[Predicate]
      val saAuthService = new SaAuthValidator {
        override val predicateBuilder = mockSaAuthPredicateBuilder

        override def authConnector = mockAuthConnector
      }

      when(mockSaAuthPredicateBuilder.authorisationPredicate(meq(validNino))(any()))
        .thenReturn(Future.successful(mockPredicate))
    }

    "invoke code block and respond with its result when auth predicate passes" in new Setup {
      when(mockAuthConnector.authorise(meq(mockPredicate), meq(EmptyRetrieval))(any(), any()))
        .thenReturn(Future.successful(()))

      val result = await(saAuthService.checkSaAuthorisation(validNino) {
        Ok("Invoked block")
      }(FakeRequest()))

      result mustBe Ok("Invoked block")
    }

    "not invoke code block and respond with UNAUTHORIZED when auth predicate fails with NoActiveSession" in new Setup {
      when(mockAuthConnector.authorise(meq(mockPredicate), meq(EmptyRetrieval))(any(), any()))
        .thenReturn(Future.failed(BearerTokenExpired()))

      val result = saAuthService.checkSaAuthorisation(validNino) {
        Ok
      }(FakeRequest())

      status(result) mustBe UNAUTHORIZED
    }

    "not invoke block and respond with FORBIDDEN when auth predicate fails with AuthorisationException" in new Setup {
      when(mockAuthConnector.authorise(meq(mockPredicate), meq(EmptyRetrieval))(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))

      val result = saAuthService.checkSaAuthorisation(validNino) {
        Ok
      }(FakeRequest())

      status(result) mustBe FORBIDDEN
    }
  }
}
