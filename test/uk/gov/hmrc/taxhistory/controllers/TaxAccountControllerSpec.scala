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

package uk.gov.hmrc.taxhistory.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.model.api.{IncomeSource, TaxAccount}
import uk.gov.hmrc.taxhistory.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.taxhistory.utils.{HttpErrors, TestRelationshipAuthService}
import uk.gov.hmrc.time.TaxYear

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class TaxAccountControllerSpec extends AnyWordSpec with Matchers with OptionValues
  with GuiceOneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  private val mockEmploymentHistoryService = mock[EmploymentHistoryService]

  private val ninoWithAgent = randomNino()
  private val ninoWithoutAgent = randomNino()

  private val testTaxAccount = TaxAccount()
  private val testTaxYear = TaxYear.current.previous.currentYear
  private val testTaxCode = "1150L"

  val cc: ControllerComponents = stubControllerComponents()
  implicit val executionContext: ExecutionContext = cc.executionContext

  override def beforeEach: Unit = {
    reset(mockEmploymentHistoryService)
  }

  val testTaxAccountController = new TaxAccountController(
    employmentHistoryService = mockEmploymentHistoryService,
    relationshipAuthService = TestRelationshipAuthService(Map(ninoWithAgent -> Arn("TestArn"))),
    cc
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
        when(mockEmploymentHistoryService.getTaxAccount(any(), any())(any[HeaderCarrier]))
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

    val testEmploymentId = UUID.randomUUID().toString
    val testIncomeSource = IncomeSource(1, 1, None, List.empty, List.empty, testTaxCode, None, 1, "")

    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getIncomeSource(any[Nino], any[TaxYear], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(testIncomeSource)))

      val result = testTaxAccountController.getIncomeSource(ninoWithAgent.nino, testTaxYear, testEmploymentId).apply(FakeRequest())
      status(result) shouldBe OK
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getIncomeSource(any(), any(), any[String])(any[HeaderCarrier]))
          .thenReturn(Future.failed(httpException))
        val result = testTaxAccountController.getIncomeSource(ninoWithAgent.nino, testTaxYear, testEmploymentId).apply(FakeRequest())
        status(result) shouldBe expectedStatus
      }
    }

    "respond with UNAUTHORIZED Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getIncomeSource(any[Nino], any[TaxYear], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(testIncomeSource)))

      val result = testTaxAccountController.getIncomeSource(ninoWithoutAgent.nino, testTaxYear, testEmploymentId).apply(FakeRequest())
      status(result) shouldBe UNAUTHORIZED
    }
  }

}
