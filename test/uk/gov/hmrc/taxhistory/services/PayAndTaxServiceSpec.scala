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
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.{HttpNotOk, TaxHistoryException}
import uk.gov.hmrc.taxhistory.model.api.{PayAndTax, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class PayAndTaxServiceSpec extends PlaySpec with MockitoSugar with TestUtil {
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
                                            |    "receivingOccupationalPension": false,
                                            |    "startDate": "21/01/2015",
                                            |    "employmentStatus":1
                                            |    }]
                                          """.stripMargin)

  val npsEmploymentResponse = npsEmploymentResponseJson.as[List[NpsEmployment]]

  lazy val iabdsResponseJson = loadFile("/json/nps/response/iabds.json")
  lazy val iabdsResponse = iabdsResponseJson.as[List[Iabd]]

  lazy val rtiEmploymentResponseJson = loadFile("/json/rti/response/dummyRti.json")
  lazy val rtiEmploymentResponse = loadFile("/json/rti/response/dummyRti.json").as[RtiData]

  "PayAndTax " should {
    "successfully populated from rti" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(iabdsResponse))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(rtiEmploymentResponse))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(TaxHistoryException(HttpNotOk(BAD_REQUEST, HttpResponse(BAD_REQUEST)))))
      val payAsYouEarn = await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))

      val payAndTax = payAsYouEarn.payAndTax
      payAndTax.size mustBe 1
    }

    "successfully retrieve payAndTaxURI from cache" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      val testPayAndTax = Json.parse(
        """ {
          |        "payAndTaxId":"2e2abe0a-8c4f-49fc-bdd2-cc13054e7172",
          |        "taxablePayTotal":2222.22,
          |        "taxTotal":111.11,
          |        "paymentDate":"2016-02-20",
          |        "paymentDate":"2016-02-20",
          |        "earlierYearUpdates":[]
          |      } """.stripMargin).as[PayAndTax]
      when(testEmploymentHistoryService.getFromCache(Matchers.any(),Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(paye))

      val payAndTax = await(testEmploymentHistoryService.getPayAndTax(Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))
      payAndTax must be(testPayAndTax)
    }

  }
}
