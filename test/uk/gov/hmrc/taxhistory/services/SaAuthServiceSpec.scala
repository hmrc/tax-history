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

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.{AnyContent, Request, Result}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth
import uk.gov.hmrc.auth.core
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{ AuthConnector, BearerTokenExpired, Enrolment, EnrolmentIdentifier, InsufficientEnrolments}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.connectors.CitizenDetailsConnector

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SaAuthServiceSpec extends PlaySpec with MockitoSugar {

  val validNino: Nino = Nino("AA000000A")

  "SaAuthorisationPredicateBuilder" should {

    val foundSaUtr = SaUtr("UtrFoundForThatNino")

    trait Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
      val mockAuthConnector: AuthConnector = mock[AuthConnector]
      val mockSaAuthService: SaAuthService = new SaAuthService(mockAuthConnector, mockCitizenDetailsConnector)

      val selfAssessmentForAgents648Predicate: Enrolment =
        Enrolment("IR-SA")
          .withIdentifier("UTR", foundSaUtr.value)
          .withDelegatedAuthRule("sa-auth")

      val individualPredicate: core.Nino = auth.core.Nino(hasNino = true, nino = Some(validNino.value))

      val agentServicesAccountWithDigitalHandshakePredicate: Enrolment =
        Enrolment("THIS-STRING-IS-NOT-RELEVANT")
          .withIdentifier("NINO", validNino.value)
          .withDelegatedAuthRule("afi-auth")
    }

    "use individual authorisation predicate if no SA UTR is found for the NINO" in new Setup {

      when(mockCitizenDetailsConnector.lookupSaUtr(meq(validNino))(any()))
        .thenReturn(Future.successful(None))

      val result: Predicate = await(mockSaAuthService.authorisationPredicate(validNino)(hc, global))

      result mustBe (individualPredicate or agentServicesAccountWithDigitalHandshakePredicate)
    }

    "use individual or agent authorisation predicate if an SA UTR is found for the NINO" in new Setup {

      when(mockCitizenDetailsConnector.lookupSaUtr(meq(validNino))(any()))
        .thenReturn(Future.successful(Some(foundSaUtr)))

      val result: Predicate = await(mockSaAuthService.authorisationPredicate(validNino)(hc, global))

      result mustBe (individualPredicate or agentServicesAccountWithDigitalHandshakePredicate or selfAssessmentForAgents648Predicate)
    }
  }

    "SaAuthService" should {
      trait Setup {
        val mockPredicate: Predicate = mock[Predicate]
        val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
        val mockAuthConnector: AuthConnector = mock[AuthConnector]
        val mockSaAuthService: SaAuthService = new SaAuthService(mockAuthConnector, mockCitizenDetailsConnector)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val request: Request[AnyContent] = FakeRequest()
        val newEnrolments = Set(
          Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "TestArn")), state="",delegatedAuthRule = None)
        )

        when(mockCitizenDetailsConnector.lookupSaUtr(meq(validNino))(any()))
          .thenReturn(Future.successful(None))
      }

      "invoke code block and respond with its result when auth predicate passes" in
        new Setup {
          when(mockAuthConnector.authorise(any(), meq(EmptyRetrieval))(any(), any()))
            .thenReturn(Future.successful(()))

          val result: Result = await(mockSaAuthService.withSaAuthorisation(validNino) { _ =>
            Future.successful(Ok("Invoked block"))
        })

        result mustBe Ok("Invoked block")
      }

      "not invoke code block and respond with UNAUTHORIZED when auth predicate fails with NoActiveSession" in new Setup {
        when(mockAuthConnector.authorise(any(), meq(EmptyRetrieval))(any(), any()))
          .thenReturn(Future.failed(BearerTokenExpired()))

        val result: Future[Result] = mockSaAuthService.withSaAuthorisation(validNino) { _ =>
          Future.successful(Ok)
        }

        status(result) mustBe UNAUTHORIZED
      }

      "not invoke block and respond with FORBIDDEN when auth predicate fails with AuthorisationException" in new Setup {
        when(mockAuthConnector.authorise(any(), meq(EmptyRetrieval))(any(), any()))
          .thenReturn(Future.failed(InsufficientEnrolments()))

        val result: Future[Result] = mockSaAuthService.withSaAuthorisation(validNino) { _ =>
          Future.successful(Ok)
        }

        status(result) mustBe FORBIDDEN
      }
    }

}
