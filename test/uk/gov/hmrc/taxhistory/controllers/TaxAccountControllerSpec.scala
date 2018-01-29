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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.api.TaxAccount
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class TaxAccountControllerSpec extends UnitSpec with OneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  val mockEmploymentHistoryService = mock[EmploymentHistoryService]
  val mockPlayAuthConnector = mock[PlayAuthConnector]
  val nino = randomNino()

  val testTaxAccount = TaxAccount()
  lazy val successResponseJson = Json.parse( """{"test":"OK"}""")
  val failureResponseJson = Json.parse( """{"reason":"Resource not found"}""")
  val errorResponseJson = Json.parse( """{"reason":"Some error."}""")
  val testTaxYear = TaxYear.current.previous.currentYear

  val newEnrolments = Set(
    Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "TestArn")), state = "", delegatedAuthRule = None)
  )
  val UnAuthorisedAgentEnrolments = Set(
    Enrolment("HMRC-AS-UNAUTHORISED-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "TestArn")), state = "", delegatedAuthRule = None)
  )

  override def beforeEach: Unit = {
    reset(mockEmploymentHistoryService)
    reset(mockPlayAuthConnector)
  }

  val testTaxAccountController = new TaxAccountController(
    employmentHistoryService = mockEmploymentHistoryService,
    authConnector = mockPlayAuthConnector
  )

  "getTaxAccount" must {

    "respond with OK for successful get" in {
      when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any(), any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(newEnrolments))))
      when(mockEmploymentHistoryService.getTaxAccount(any[Nino], any[TaxYear])(any[HeaderCarrier]))
        .thenReturn(Future.successful(testTaxAccount))

      val result = testTaxAccountController.getTaxAccount(nino.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe OK
    }

    "respond with NOT_FOUND, for unsuccessful GET" in {
      when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any(), any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(newEnrolments))))
      when(mockEmploymentHistoryService.getTaxAccount(any[Nino], any[TaxYear])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new NotFoundException("")))

      val result = testTaxAccountController.getTaxAccount(nino.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe NOT_FOUND
    }

    "respond with BAD_REQUEST, if HODS/Downstream sends BadRequest status" in {
      when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any(), any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(newEnrolments))))
      when(mockEmploymentHistoryService.getTaxAccount(any[Nino], any[TaxYear])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("")))

      val result = testTaxAccountController.getTaxAccount(nino.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe BAD_REQUEST
    }

    "respond with SERVICE_UNAVAILABLE, if HODS/Downstream is unavailable" in {
      when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any(), any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(newEnrolments))))
      when(mockEmploymentHistoryService.getTaxAccount(any[Nino], any[TaxYear])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = testTaxAccountController.getTaxAccount(nino.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe SERVICE_UNAVAILABLE
    }

    "respond with INTERNAL_SERVER_ERROR, if HODS/Downstream sends some server error response" in {
      when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any(), any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(newEnrolments))))
      when(mockEmploymentHistoryService.getTaxAccount(any[Nino], any[TaxYear])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = testTaxAccountController.getTaxAccount(nino.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "respond with UNAUTHORIZED Status for enrolments which is not HMRC Agent" in {
      when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any(), any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(UnAuthorisedAgentEnrolments))))
      when(mockEmploymentHistoryService.getTaxAccount(any[Nino], any[TaxYear])(any[HeaderCarrier]))
        .thenReturn(Future.successful(testTaxAccount))

      val result = testTaxAccountController.getTaxAccount(nino.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe UNAUTHORIZED
    }

    "respond with UNAUTHORIZED Status where affinity group is not retrieved" in {
      when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any(), any()))
        .thenReturn(Future.successful(new ~[Option[AffinityGroup], Enrolments](None, Enrolments(UnAuthorisedAgentEnrolments))))
      when(mockEmploymentHistoryService.getTaxAccount(any[Nino], any[TaxYear])(any[HeaderCarrier]))
        .thenReturn(Future.successful(testTaxAccount))

      val result = testTaxAccountController.getTaxAccount(nino.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe UNAUTHORIZED
    }

    "respond with UNAUTHORIZED Status when the user is not logged in" in {
      when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any(), any()))
        .thenReturn(Future.failed(new InsufficientEnrolments))

      val result = testTaxAccountController.getTaxAccount(nino.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe UNAUTHORIZED
    }

    "respond with INTERNAL_SERVER_ERROR Status when there is an auth error" in {
      when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any(), any()))
        .thenReturn(Future.failed(new MissingBearerToken))

      val result = testTaxAccountController.getTaxAccount(nino.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
