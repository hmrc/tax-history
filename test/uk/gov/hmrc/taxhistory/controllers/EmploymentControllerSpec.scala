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

package uk.gov.hmrc.taxhistory.controllers

import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.model.api.Employment
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.taxhistory.utils.{HttpErrors, TestRelationshipAuthService}

import scala.concurrent.Future

class EmploymentControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  val mockEmploymentHistoryService: EmploymentHistoryService = mock[EmploymentHistoryService]

  val ninoWithAgent = randomNino()
  val ninoWithoutAgent = randomNino()

  val testEmployment = Employment(startDate = Some(LocalDate.now()),
    payeReference = "SOME_PAYE", employerName = "Megacorp Plc",
    employmentStatus = EmploymentStatus.Live, worksNumber = "00191048716")
  val testEmployments = List(testEmployment)

  override def beforeEach = {
    reset(mockEmploymentHistoryService)
  }

  val testEmploymentController = new EmploymentController(
    employmentHistoryService = mockEmploymentHistoryService,
    relationshipAuthService = TestRelationshipAuthService(Map(ninoWithAgent -> Arn("TestArn")))
  )

  "getEmployments" must {

    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getEmployments(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(testEmployments))
      val result = testEmploymentController.getEmployments(ninoWithAgent.nino, 2016).apply(FakeRequest())
      status(result) must be(OK)
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getEmployments(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.failed(httpException))
        val result = testEmploymentController.getEmployments(ninoWithAgent.nino, 2015).apply(FakeRequest())
        status(result) must be(expectedStatus)
      }
    }

    "respond with Unauthorised Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getEmployments(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(testEmployments))
      val result = testEmploymentController.getEmployments(ninoWithoutAgent.nino, 2015).apply(FakeRequest())
      status(result) must be(UNAUTHORIZED)
    }

  }

  "getEmployment" must {

    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getEmployment(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(testEmployment))
      val result = testEmploymentController.getEmployment(ninoWithAgent.nino, 2016,"ba047b92-6899-4bf8-819a-820fc0dd2703").apply(FakeRequest())
      status(result) must be(OK)
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getEmployment(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.failed(httpException))
        val result = testEmploymentController.getEmployment(ninoWithAgent.nino, 2015,"ba047b92-6899-4bf8-819a-820fc0dd2703").apply(FakeRequest())
        status(result) must be(expectedStatus)
      }
    }

    "respond with Unauthorised Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getEmployment(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(testEmployment))
      val result = testEmploymentController.getEmployment(ninoWithoutAgent.nino, 2015,"ba047b92-6899-4bf8-819a-820fc0dd2703").apply(FakeRequest())
      status(result) must be(UNAUTHORIZED)
    }
  }

}
