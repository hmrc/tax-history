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
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.taxhistory.model.api.Allowance
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService
import uk.gov.hmrc.taxhistory.utils.HttpErrors

import scala.concurrent.Future

class AllowanceControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  private val mockEmploymentHistoryService = mock[EmploymentHistoryService]
  private val mockPlayAuthConnector = mock[PlayAuthConnector]
  private val nino = randomNino()
  
  val testAllowances = List(Allowance(iabdType = "CarBenefit", amount = BigDecimal(100.00)))

  override def beforeEach = {
    reset(mockEmploymentHistoryService)
    reset(mockPlayAuthConnector)
  }

  val testAllowanceController = new AllowanceController(
    employmentHistoryService = mockEmploymentHistoryService,
    authConnector = mockPlayAuthConnector
  ) {
    // This takes care of the authentication and the verification of the existing NINO-Agent relationship
    override def retrieveArnFor(nino: String)(implicit hc: HeaderCarrier): Future[Option[Arn]] = Future.successful(Some(Arn("TestArn")))
  }

  val noArnAllowanceController = new AllowanceController(
    employmentHistoryService = mockEmploymentHistoryService,
    authConnector = mockPlayAuthConnector
  ) {
    // This always returns no ARN for the given NINO, which means that there is no existing NINO-Agent relationship
    override def retrieveArnFor(nino: String)(implicit hc: HeaderCarrier): Future[Option[Arn]] = Future.successful(None)
  }

  "getAllowances" must {

    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getAllowances(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(testAllowances))

      val result = testAllowanceController.getAllowances(nino.nino, 2016).apply(FakeRequest())
      status(result) must be(OK)
    }

    "propagate error responses from upstream microservices" in {
      HttpErrors.toCheck.foreach { case (httpException, expectedStatus) =>
        when(mockEmploymentHistoryService.getAllowances(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.failed(httpException))
        val result = testAllowanceController.getAllowances(nino.nino, 2016).apply(FakeRequest())
        status(result) must be(expectedStatus)
      }
    }

    "respond with Unauthorised Status for enrolments which is not HMRC Agent" in {
      when(mockEmploymentHistoryService.getAllowances(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(testAllowances))
      val result = noArnAllowanceController.getAllowances(nino.nino, 2016).apply(FakeRequest())
      status(result) must be(UNAUTHORIZED)
    }
  }

}
