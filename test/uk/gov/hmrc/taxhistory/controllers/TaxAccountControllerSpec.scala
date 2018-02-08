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
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.api.TaxAccount
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.{EmploymentHistoryService, RelationshipAuthService}
import uk.gov.hmrc.taxhistory.utils.{HttpErrors, TestRelationshipAuthService}
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class TaxAccountControllerSpec extends UnitSpec with OneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  val mockEmploymentHistoryService = mock[EmploymentHistoryService]

  val ninoWithAgent = randomNino()
  val ninoWithoutAgent = randomNino()

  val testTaxAccount = TaxAccount()

  val testTaxYear = TaxYear.current.previous.currentYear

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
        .thenReturn(Future.successful(testTaxAccount))

      val result = testTaxAccountController.getTaxAccount(ninoWithAgent.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe OK
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.failed(httpException))
        val result = testTaxAccountController.getTaxAccount(ninoWithAgent.nino, testTaxYear).apply(FakeRequest())
        status(result) shouldBe (expectedStatus)
      }
    }

    "respond with UNAUTHORIZED Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getTaxAccount(any[Nino], any[TaxYear])(any[HeaderCarrier]))
        .thenReturn(Future.successful(testTaxAccount))

      val result = testTaxAccountController.getTaxAccount(ninoWithoutAgent.nino, testTaxYear).apply(FakeRequest())
      status(result) shouldBe UNAUTHORIZED
    }
  }

}
