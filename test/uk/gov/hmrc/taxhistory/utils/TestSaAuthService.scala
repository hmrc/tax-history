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

package uk.gov.hmrc.taxhistory.utils

import java.time.LocalDate
import org.mockito.Mockito
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.auth
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.connectors.CitizenDetailsConnector
import uk.gov.hmrc.taxhistory.model.api.{Employment, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.taxhistory.services.SaAuthService
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
  * A test version of SaAuthService which returns a predicate without calling citizenDetailsConnector
  * rather than interrogating a real auth service.
  */
case class TestSaAuthService()
    extends SaAuthService(
      authConnector = Mockito.mock(classOf[AuthConnector]),
      citizenDetailsConnector = Mockito.mock(classOf[CitizenDetailsConnector])
    )
    with TestUtil {

  val testEmploymentId: UUID   = java.util.UUID.randomUUID
  val testStartDate: LocalDate = LocalDate.now()
  val testPaye: PayAsYouEarn   =
    PayAsYouEarn(
      employments = List(
        Employment(
          employmentId = testEmploymentId,
          startDate = Some(testStartDate),
          payeReference = "SOME_PAYE",
          employerName = "Megacorp Plc",
          employmentStatus = EmploymentStatus.Live,
          worksNumber = "00191048716"
        )
      ),
      allowances = List.empty,
      incomeSources = Map.empty,
      benefits = Map.empty,
      payAndTax = Map.empty,
      taxAccount = None,
      statePension = None
    )
  val validNino: Nino          = randomNino()
  val unauthorisedNino: Nino   = randomNino()
  val forbiddenNino: Nino      = randomNino()

  override def authorisationPredicate(
    nino: Nino
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Predicate] = {

    val checkIndividual: Predicate = auth.core.Nino(hasNino = true, nino = Some(nino.value))

    val checkAgentServicesWithDigitalHandshake: Predicate =
      Enrolment("THIS-STRING-IS-NOT-RELEVANT")
        .withIdentifier("NINO", nino.value)
        .withDelegatedAuthRule("afi-auth")

    Future.successful(checkIndividual or checkAgentServicesWithDigitalHandshake)
  }

  override def withSaAuthorisation(nino: Nino)(
    action: Request[AnyContent] => Future[Result]
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    nino.nino match {
      case validNino.nino        => Future.successful(Ok(PayAsYouEarn.formats.writes(testPaye)))
      case unauthorisedNino.nino => Future.successful(Unauthorized)
      case forbiddenNino.nino    => Future.successful(Forbidden)
      case _                     => Future.successful(BadRequest)
    }

}
