/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.api.PayAndTax
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.taxhistory.utils.{HttpErrors, TestRelationshipAuthService}

import scala.concurrent.Future

class PayAndTaxControllerSpec extends UnitSpec with OneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  val mockEmploymentHistoryService: EmploymentHistoryService = mock[EmploymentHistoryService]

  val ninoWithAgent = randomNino()
  val ninoWithoutAgent = randomNino()

  val taxYear = 2016
  val employmentId: String = UUID.randomUUID().toString

  val testPayAndTax = PayAndTax(earlierYearUpdates = Nil)
  val testPayAndTaxMap = Map(s"${testPayAndTax.payAndTaxId}" -> testPayAndTax, s"${testPayAndTax.payAndTaxId}" -> testPayAndTax)

  override def beforeEach: Unit = {
    reset(mockEmploymentHistoryService)
  }

  val testPayAndTaxController = new PayAndTaxController(
    employmentHistoryService = mockEmploymentHistoryService,
    relationshipAuthService = TestRelationshipAuthService(Map(ninoWithAgent -> Arn("TestArn")))
  )

  "getPayAndTax" should {
    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getPayAndTax(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(testPayAndTax)))
      val result = testPayAndTaxController.getPayAndTax(ninoWithAgent.nino, taxYear, employmentId).apply(FakeRequest())
      status(result) shouldBe OK
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getPayAndTax(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.failed(httpException))
        val result = testPayAndTaxController.getPayAndTax(ninoWithAgent.nino, taxYear, employmentId).apply(FakeRequest())
        status(result) shouldBe expectedStatus
      }
    }

    "respond with Unauthorised Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getPayAndTax(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(testPayAndTax)))
      val result = testPayAndTaxController.getPayAndTax(ninoWithoutAgent.nino, taxYear, employmentId).apply(FakeRequest())
      status(result) shouldBe UNAUTHORIZED
    }
  }

  "getAllPayAndTax" should {
    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getAllPayAndTax(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(testPayAndTaxMap))
      val result = testPayAndTaxController.getAllPayAndTax(ninoWithAgent.nino, taxYear).apply(FakeRequest())
      status(result) shouldBe OK
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getAllPayAndTax(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.failed(httpException))
        val result = testPayAndTaxController.getAllPayAndTax(ninoWithAgent.nino, taxYear).apply(FakeRequest())
        status(result) shouldBe expectedStatus
      }
    }

    "respond with Unauthorised Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getAllPayAndTax(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(testPayAndTaxMap))
      val result = testPayAndTaxController.getPayAndTax(ninoWithoutAgent.nino, taxYear, employmentId).apply(FakeRequest())
      status(result) shouldBe UNAUTHORIZED
    }
  }

}
