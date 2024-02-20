/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.taxhistory.utils.TestUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RelationshipAuthServiceSpec extends PlaySpec with MockitoSugar with TestUtil {

  import org.mockito.Mockito._

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAuthConnector           = mock[AuthConnector]
  val testRelationshipAuthService = new RelationshipAuthService(mockAuthConnector)

  val ninoWithAgent = randomNino()

  val newEnrolments               = Set(
    Enrolment(
      "HMRC-AS-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", "TestArn")),
      state = "",
      delegatedAuthRule = None
    )
  )
  val UnAuthorisedAgentEnrolments = Set(
    Enrolment(
      "HMRC-AS-UNAUTHORISED-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", "TestArn")),
      state = "",
      delegatedAuthRule = None
    )
  )

  "RelationshipAuthService" should {

    "authorise when a correct relationship is present" in {
      when(mockAuthConnector.authorise(any(), any[Retrieval[Option[AffinityGroup] ~ Enrolments]]())(any(), any()))
        .thenReturn(
          Future.successful(
            new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(newEnrolments))
          )
        )

      val result = testRelationshipAuthService.withAuthorisedRelationship(ninoWithAgent) { arn =>
        Future.successful(Ok("Some content"))
      }

      status(result) must be(OK)
    }

    "respond with UNAUTHORIZED for enrolments which are not HMRC Agents" in {

      when(mockAuthConnector.authorise(any(), any[Retrieval[Option[AffinityGroup] ~ Enrolments]]())(any(), any()))
        .thenReturn(
          Future.successful(
            new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(UnAuthorisedAgentEnrolments))
          )
        )

      val result = testRelationshipAuthService.withAuthorisedRelationship(ninoWithAgent) { arn =>
        Future.successful(Ok("Some content"))
      }

      status(result) must be(UNAUTHORIZED)
    }

    "respond with UNAUTHORIZED where the affinity group is not retrieved" in {
      when(mockAuthConnector.authorise(any(), any[Retrieval[Option[AffinityGroup] ~ Enrolments]]())(any(), any()))
        .thenReturn(Future.failed(new UnauthorizedException("Failed to retrieve affinity group or enrolments")))

      val result = testRelationshipAuthService.withAuthorisedRelationship(ninoWithAgent) { arn =>
        Future.successful(Ok("Some content"))
      }

      status(result) must be(UNAUTHORIZED)
    }

    "respond with UNAUTHORIZED when the user is not logged in" in {
      when(mockAuthConnector.authorise(any(), any[Retrieval[Option[AffinityGroup] ~ Enrolments]]())(any(), any()))
        .thenReturn(Future.failed(new UnauthorizedException("Unauthorized")))

      val result = testRelationshipAuthService.withAuthorisedRelationship(ninoWithAgent) { arn =>
        Future.successful(Ok("Some content"))
      }

      status(result) must be(UNAUTHORIZED)
    }

    "respond with UNAUTHORIZED when there are insufficient enrolments" in {
      when(mockAuthConnector.authorise(any(), any[Retrieval[Option[AffinityGroup] ~ Enrolments]]())(any(), any()))
        .thenReturn(Future.failed(new InsufficientEnrolments))

      val result = testRelationshipAuthService.withAuthorisedRelationship(ninoWithAgent) { arn =>
        Future.successful(Ok("Some content"))
      }

      status(result) must be(UNAUTHORIZED)
    }

    "respond with INTERNAL_SERVER_ERROR when there is an auth error" in {
      when(mockAuthConnector.authorise(any(), any[Retrieval[Option[AffinityGroup] ~ Enrolments]]())(any(), any()))
        .thenReturn(Future.failed(new MissingBearerToken))

      val result = testRelationshipAuthService.withAuthorisedRelationship(ninoWithAgent) { arn =>
        Future.successful(Ok("Some content"))
      }

      status(result) must be(INTERNAL_SERVER_ERROR)
    }
  }

}
