/*
 * Copyright 2004 HM Revenue & Customs
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

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.api.IndividualTaxYear
import uk.gov.hmrc.taxhistory.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.taxhistory.utils.{HttpErrors, TestRelationshipAuthService}

import scala.concurrent.{ExecutionContext, Future}

class IndividualTaxYearControllerSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with MockitoSugar
    with TestUtil
    with BeforeAndAfterEach {

  val mockEmploymentHistoryService: EmploymentHistoryService = mock[EmploymentHistoryService]

  val ninoWithAgent: Nino    = randomNino()
  val ninoWithoutAgent: Nino = randomNino()

  private val taxYear = 2018

  private val mockAuditable                 = mock[Auditable]
  val testTaxYears: List[IndividualTaxYear] = List(IndividualTaxYear(taxYear, "fakeUri", "fakeUri", "fakeUri"))

  val cc: ControllerComponents                    = stubControllerComponents()
  implicit val executionContext: ExecutionContext = cc.executionContext

  override def beforeEach(): Unit =
    reset(mockEmploymentHistoryService)

  val testIndividualTaxYearController = new IndividualTaxYearController(
    employmentHistoryService = mockEmploymentHistoryService,
    relationshipAuthService = TestRelationshipAuthService(Map(ninoWithAgent -> Arn("TestArn"))),
    auditable = mockAuditable,
    cc = cc
  )

  "getTaxYears" must {
    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getTaxYears)
        .thenReturn(Future.successful(testTaxYears))
      val result = testIndividualTaxYearController.getTaxYears(ninoWithAgent.nino).apply(FakeRequest())
      status(result) must be(OK)
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getTaxYears)
          .thenReturn(Future.failed(httpException))
        val result = testIndividualTaxYearController.getTaxYears(ninoWithAgent.nino).apply(FakeRequest())
        status(result) must be(expectedStatus)
      }
    }

    "respond with Unauthorised Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getTaxYears)
        .thenReturn(Future.successful(testTaxYears))
      val result = testIndividualTaxYearController.getTaxYears(ninoWithoutAgent.nino).apply(FakeRequest())
      status(result) must be(UNAUTHORIZED)
    }
  }

}
