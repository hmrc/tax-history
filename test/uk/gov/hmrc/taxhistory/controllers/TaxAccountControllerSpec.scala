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

import java.util.UUID

import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.api.{IncomeSource, TaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.taxhistory.utils.{HttpErrors, TestRelationshipAuthService}
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class TaxAccountControllerSpec extends UnitSpec with OneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  private val mockEmploymentHistoryService = mock[EmploymentHistoryService]

  private val ninoWithAgent = randomNino()
  private val ninoWithoutAgent = randomNino()

  private val testTaxAccount = TaxAccount()
  private val testTaxYear = TaxYear.current.previous.currentYear
  private val testTaxCode = "1150L"

  override def beforeEach: Unit = {
    reset(mockEmploymentHistoryService)
  }

  val testTaxAccountController = new TaxAccountController(
    employmentHistoryService = mockEmploymentHistoryService,
    relationshipAuthService = TestRelationshipAuthService(Map(ninoWithAgent -> Arn("TestArn")))
  )

  "getTaxAccount" must {

    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getTaxAccount(any[Nino], any[TaxYear])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(testTaxAccount)))

      val result = testTaxAccountController.getTaxAccount(ninoWithAgent.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe OK
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.failed(httpException))
        val result = testTaxAccountController.getTaxAccount(ninoWithAgent.nino, testTaxYear).apply(FakeRequest())
        status(result) shouldBe expectedStatus
      }
    }

    "respond with UNAUTHORIZED Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getTaxAccount(any[Nino], any[TaxYear])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(testTaxAccount)))

      val result = testTaxAccountController.getTaxAccount(ninoWithoutAgent.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe UNAUTHORIZED
    }
  }

  "getIncomeSource" must {

    val testEmnploymentId = UUID.randomUUID().toString
    val testIncomeSource = IncomeSource(1, 1, None, List.empty, List.empty, testTaxCode, None, 1, "")

    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getIncomeSource(any[Nino], any[TaxYear], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(testIncomeSource)))

      val result = testTaxAccountController.getIncomeSource(ninoWithAgent.nino, testTaxYear, testEmnploymentId).apply(FakeRequest())
      status(result) shouldBe OK
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getIncomeSource(Matchers.any(), Matchers.any(), any[String])(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.failed(httpException))
        val result = testTaxAccountController.getIncomeSource(ninoWithAgent.nino, testTaxYear, testEmnploymentId).apply(FakeRequest())
        status(result) shouldBe expectedStatus
      }
    }

    "respond with UNAUTHORIZED Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getIncomeSource(any[Nino], any[TaxYear], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(testIncomeSource)))

      val result = testTaxAccountController.getIncomeSource(ninoWithoutAgent.nino, testTaxYear, testEmnploymentId).apply(FakeRequest())
      status(result) shouldBe UNAUTHORIZED
    }
  }

}
