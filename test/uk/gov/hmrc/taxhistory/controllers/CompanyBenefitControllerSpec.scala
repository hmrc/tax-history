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

package uk.gov.hmrc.taxhistory.controllers

import java.util.UUID

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.model.api.CompanyBenefit
import uk.gov.hmrc.taxhistory.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.taxhistory.utils.{HttpErrors, TestRelationshipAuthService}

import scala.concurrent.{ExecutionContext, Future}

class CompanyBenefitControllerSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with MockitoSugar
    with TestUtil
    with BeforeAndAfterEach {
  val mockEmploymentHistoryService: EmploymentHistoryService = mock[EmploymentHistoryService]

  val ninoWithAgent: Nino    = randomNino()
  val ninoWithoutAgent: Nino = randomNino()

  private val taxYear = 2016

  val testCompanyBenefits = List(CompanyBenefit(iabdType = "CarBenefit", amount = BigDecimal(100.00)))

  val employmentId: String = UUID.randomUUID().toString

  val cc: ControllerComponents                    = stubControllerComponents()
  implicit val executionContext: ExecutionContext = cc.executionContext

  override def beforeEach: Unit =
    reset(mockEmploymentHistoryService)

  val testCompanyBenefitController = new CompanyBenefitController(
    employmentHistoryService = mockEmploymentHistoryService,
    relationshipAuthService = TestRelationshipAuthService(Map(ninoWithAgent -> Arn("TestArn"))),
    cc = cc
  )

  "getBenefits" must {

    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getCompanyBenefits(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(testCompanyBenefits))
      val result =
        testCompanyBenefitController.getCompanyBenefits(ninoWithAgent.nino, taxYear, employmentId).apply(FakeRequest())
      status(result) must be(OK)
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getCompanyBenefits(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.failed(httpException))
        val result =
          testCompanyBenefitController
            .getCompanyBenefits(ninoWithAgent.nino, taxYear, employmentId)
            .apply(FakeRequest())
        status(result) must be(expectedStatus)
      }
    }

    "respond with Unauthorised Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getCompanyBenefits(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(testCompanyBenefits))
      val result =
        testCompanyBenefitController
          .getCompanyBenefits(ninoWithoutAgent.nino, taxYear, employmentId)
          .apply(FakeRequest())
      status(result) must be(UNAUTHORIZED)
    }
  }

}
