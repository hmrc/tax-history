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

package uk.gov.hmrc.taxhistory.services

import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.taxhistory.model.api.PayAsYouEarn
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class AllowancesServiceSpec extends PlaySpec with MockitoSugar with TestUtil {

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()

  val testEmploymentHistoryService = TestEmploymentHistoryService.createNew

  val failureResponseJson = Json.parse("""{"reason":"Bad Request"}""")

  val npsEmploymentResponse =  Json.parse(""" [{
                             |    "nino": "AA000000",
                             |    "sequenceNumber": 1,
                             |    "worksNumber": "6044041000000",
                             |    "taxDistrictNumber": "531",
                             |    "payeNumber": "J4816",
                             |    "employerName": "Aldi",
                             |    "receivingJobseekersAllowance" : false,
                             |    "otherIncomeSourceIndicator" : false,
                             |    "receivingOccupationalPension": true,
                             |    "employmentStatus": 1,
                             |    "startDate": "21/01/2015"
                             |    }]
                           """.stripMargin)


  lazy val iabdsJsonResponse = loadFile("/json/nps/response/iabds.json")



  "Allowances " should {
    "successfully  populated  from iabds" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentResponse))))
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(iabdsJsonResponse))))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND)))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND)))
      val response =  await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
      response mustBe a[HttpResponse]
      response.status mustBe OK
      val payAsYouEarn = response.json.as[PayAsYouEarn]
      val allowances = payAsYouEarn.allowances
      allowances.size mustBe 1
    }

    "successfully retrieve allowance from cache" in {
      lazy val payeJson = loadFile("/json/model/api/paye.json")

      val allowanceJson = Json.parse(
        """ [
           {
               "allowanceId": "c9923a63-4208-4e03-926d-7c7c88adc7ee",
               "iabdType": "payeType",
               "amount": 12
          }
          ] """.stripMargin)
      when(testEmploymentHistoryService.getFromCache(Matchers.any(),Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Some(payeJson)))

      val result = await(testEmploymentHistoryService.getAllowances("AA000000A", 2014))
      result.json must be(allowanceJson)
    }

    "return empty array when failed to fetch allowance from cache" in {
      lazy val payeJson = Json.arr()

      val allowanceJson = Json.parse(
        """ [
          ] """.stripMargin)
      when(testEmploymentHistoryService.getFromCache(Matchers.any(),Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Some(payeJson)))

      val result = await(testEmploymentHistoryService.getAllowances("AA000000A", 2014))
      result.status must be(NOT_FOUND)
      result.json must be(allowanceJson)
    }
  }
}
