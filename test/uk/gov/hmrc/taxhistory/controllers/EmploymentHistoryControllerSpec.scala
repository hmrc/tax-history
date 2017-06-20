/*
 * Copyright 2017 HM Revenue & Customs
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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService

import scala.concurrent.Future

class EmploymentHistoryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with TestUtil with BeforeAndAfterEach {

  private val mockEmploymentHistoryService = mock[EmploymentHistoryService]
  private val successResponseJson = Json.parse( """{"test":"OK"}""")
  private val failureResponseJson = Json.parse( """{"reason":"Resource not found"}""")
  private val errorResponseJson = Json.parse( """{"reason":"Some Error."}""")


  override def beforeEach = {
    reset(mockEmploymentHistoryService)
  }

  object TestEmploymentHistoryController extends EmploymentHistoryController{
    override def employmentHistoryService: EmploymentHistoryService = mockEmploymentHistoryService

  }

  "getOverpaymentStatus " must {
    "respond with OK for successful get" in {
      when(mockEmploymentHistoryService.getEmploymentHistory(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(successResponseJson))))
      val result = TestEmploymentHistoryController.getEmploymentHistory(randomNino.toString(), 2015).apply(FakeRequest())
      status(result) must be(OK)
    }

    "respond with NOT_FOUND, for unsuccessful GET" in {
      when(mockEmploymentHistoryService.getEmploymentHistory(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(failureResponseJson))))
      val result = TestEmploymentHistoryController.getEmploymentHistory(randomNino.toString(), 2015).apply(FakeRequest())
      status(result) must be(NOT_FOUND)
    }

    "respond with BAD_REQUEST, if HODS/Downstream sends BadRequest status" in {
      when(mockEmploymentHistoryService.getEmploymentHistory(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(errorResponseJson))))
      val result = TestEmploymentHistoryController.getEmploymentHistory(randomNino.toString(), 2015).apply(FakeRequest())
      status(result) must be(BAD_REQUEST)
    }

    "respond with SERVICE_UNAVAILABLE, if HODS/Downstream is unavailable" in {
      when(mockEmploymentHistoryService.getEmploymentHistory(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(errorResponseJson))))
      val result = TestEmploymentHistoryController.getEmploymentHistory(randomNino.toString(), 2015).apply(FakeRequest())
      status(result) must be(SERVICE_UNAVAILABLE)
    }

    "respond with InternalServerError, if HODS/Downstream  sends some server error response" in {
      when(mockEmploymentHistoryService.getEmploymentHistory(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(errorResponseJson))))
      val result = TestEmploymentHistoryController.getEmploymentHistory(randomNino.toString(), 2015).apply(FakeRequest())
      status(result) must be(INTERNAL_SERVER_ERROR)
    }
  }

}
