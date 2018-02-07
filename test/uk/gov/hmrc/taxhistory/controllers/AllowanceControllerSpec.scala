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

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.taxhistory.model.api.Allowance
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService

import scala.concurrent.Future

class AllowanceControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  private val mockEmploymentHistoryService = mock[EmploymentHistoryService]
  private val mockPlayAuthConnector = mock[PlayAuthConnector]
  private val nino = randomNino()
  
  val testAllowances = List(Allowance(iabdType = "CarBenefit", amount = BigDecimal(100.00)))

  val newEnrolments = Set(
    Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "TestArn")), state = "", delegatedAuthRule = None)
  )
  val UnAuthorisedAgentEnrolments = Set(
    Enrolment("HMRC-AS-UNAUTHORISED-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "TestArn")), state = "", delegatedAuthRule = None)
  )
  override def beforeEach = {
    reset(mockEmploymentHistoryService)
    reset(mockPlayAuthConnector)
  }

  val testAllowanceController = new AllowanceController(
    employmentHistoryService = mockEmploymentHistoryService,
    authConnector = mockPlayAuthConnector
  )

  "getAllowances" must {

    "respond with OK for successful get" in {
      when(mockPlayAuthConnector.authorise(Matchers.any(),Matchers.any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())
      (Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent) , Enrolments(newEnrolments))))
      when(mockEmploymentHistoryService.getAllowances(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(testAllowances))

      val result = testAllowanceController.getAllowances(nino.nino, 2016).apply(FakeRequest())
      status(result) must be(OK)
    }

    "respond with NOT_FOUND, for unsuccessful GET" in {
      when(mockPlayAuthConnector.authorise(Matchers.any(),Matchers.any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())
      (Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent) , Enrolments(newEnrolments))))
      when(mockEmploymentHistoryService.getAllowances(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new NotFoundException("")))
      val result = testAllowanceController.getAllowances(nino.nino, 2016).apply(FakeRequest())
      status(result) must be(NOT_FOUND)
    }

    "respond with BAD_REQUEST, if HODS/Downstream sends BadRequest status" in {
      when(mockPlayAuthConnector.authorise(Matchers.any(),Matchers.any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())
      (Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent) , Enrolments(newEnrolments))))
      when(mockEmploymentHistoryService.getAllowances(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("")))
      val result = testAllowanceController.getAllowances(nino.nino, 2016).apply(FakeRequest())
      status(result) must be(BAD_REQUEST)
    }

    "respond with SERVICE_UNAVAILABLE, if HODS/Downstream is unavailable" in {
      when(mockPlayAuthConnector.authorise(Matchers.any(),Matchers.any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())
      (Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent) , Enrolments(newEnrolments))))
      when(mockEmploymentHistoryService.getAllowances(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))
      val result = testAllowanceController.getAllowances(nino.nino, 2016).apply(FakeRequest())
      status(result) must be(SERVICE_UNAVAILABLE)
    }

    "respond with InternalServerError, if HODS/Downstream  sends some server error response" in {
      when(mockPlayAuthConnector.authorise(Matchers.any(),Matchers.any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())
      (Matchers.any(),Matchers.any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent) , Enrolments(newEnrolments))))
      when(mockEmploymentHistoryService.getAllowances(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
      val result = testAllowanceController.getAllowances(nino.nino, 2016).apply(FakeRequest())
      status(result) must be(INTERNAL_SERVER_ERROR)
    }

    "respond with Unauthorised Status for enrolments which is not HMRC Agent" in {
      when(mockPlayAuthConnector.authorise(Matchers.any(), Matchers.any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())
      (Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(UnAuthorisedAgentEnrolments))))

      when(mockEmploymentHistoryService.getAllowances(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(testAllowances))
      val result = testAllowanceController.getAllowances(nino.nino, 2016).apply(FakeRequest())
      status(result) must be(UNAUTHORIZED)
    }

    "respond with Unauthorised Status where affinity group is not retrieved" in {
      when(mockPlayAuthConnector.authorise(Matchers.any(), Matchers.any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())
      (Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](None, Enrolments(UnAuthorisedAgentEnrolments))))

      when(mockEmploymentHistoryService.getAllowances(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(testAllowances))
      val result = testAllowanceController.getAllowances(nino.nino, 2016).apply(FakeRequest())
      status(result) must be(UNAUTHORIZED)
    }
  }

}
