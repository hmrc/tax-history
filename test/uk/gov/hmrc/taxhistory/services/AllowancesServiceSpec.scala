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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.taxhistory.{HttpNotOk, TaxHistoryException}
import uk.gov.hmrc.taxhistory.model.api.{Allowance, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class AllowancesServiceSpec extends PlaySpec with MockitoSugar with TestUtil {

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()

  val testEmploymentHistoryService = TestEmploymentHistoryService.createNew()

  val failureResponseJson = Json.parse("""{"reason":"Bad Request"}""")

  val npsEmploymentResponseJson =  Json.parse(""" [{
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

  val npsEmploymentResponse = npsEmploymentResponseJson.as[List[NpsEmployment]]

  lazy val iabdsResponseJson = loadFile("/json/nps/response/iabds.json")
  lazy val iabdsResponse = iabdsResponseJson.as[List[Iabd]]



  "Allowances" should {
    "successfully populated from iabds" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(iabdsResponse))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(TaxHistoryException(HttpNotOk(NOT_FOUND, HttpResponse(NOT_FOUND)))))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(TaxHistoryException(HttpNotOk(NOT_FOUND, HttpResponse(NOT_FOUND)))))
      val response =  await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))

      val allowances = response.allowances
      allowances.size mustBe 1
    }

    "successfully retrieve allowance from cache" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      val allowance = Json.parse(
        """ [
           {
               "allowanceId": "c9923a63-4208-4e03-926d-7c7c88adc7ee",
               "iabdType": "payeType",
               "amount": 12
          }
          ] """.stripMargin).as[List[Allowance]]
      when(testEmploymentHistoryService.getFromCache(Matchers.any(),Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(paye))

      val result = await(testEmploymentHistoryService.getAllowances(Nino("AA000000A"), TaxYear(2014)))
      result must be(allowance)
    }

  }
}
