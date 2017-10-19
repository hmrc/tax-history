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

package uk.gov.hmrc.taxhistory.services

import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.api.PayAsYouEarn
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class PayAndTaxServiceSpec extends PlaySpec with MockitoSugar with TestUtil{
  private val mockNpsConnector= mock[NpsConnector]
  private val mockRtiDataConnector= mock[RtiConnector]
  private val mockAudit= mock[Audit]
  private val mockCache = mock[TaxHistoryCacheService]

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()
  object TestEmploymentService extends EmploymentHistoryService {
    override def npsConnector: NpsConnector = mockNpsConnector
    override def rtiConnector: RtiConnector = mockRtiDataConnector

    override def cacheService: TaxHistoryCacheService = mockCache

    override def audit: Audit = mockAudit
  }

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
                                            |    "startDate": "21/01/2015"
                                            |    }]
                                          """.stripMargin)

  lazy val iabdsJsonResponse = loadFile("/json/nps/response/iabds.json")
  lazy val rtiEmploymentResponse = loadFile("/json/rti/response/dummyRti.json")

  "PayAndTax " should {
    "successfully  populated  from rti" in {
      when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentResponse))))
      when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(iabdsJsonResponse))))
      when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK,Some(rtiEmploymentResponse))))
      val response =  await(TestEmploymentService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
      response mustBe a[HttpResponse]
      response.status mustBe OK
      val payAsYouEarn = response.json.as[PayAsYouEarn]
      val payAndTax = payAsYouEarn.payAndTax
      payAndTax.size mustBe 1
    }

    "successfully retrieve payAndTaxURI from cache" in {
      lazy val payeJson = loadFile("/json/model/api/paye.json")

      val payAndTaxJson = Json.parse(
        """ {
          |        "payAndTaxId":"2e2abe0a-8c4f-49fc-bdd2-cc13054e7172",
          |        "taxablePayTotal":2222.22,
          |        "taxTotal":111.11,
          |        "earlierYearUpdates":[]
          |      } """.stripMargin)
      when(TestEmploymentService.getFromCache(Matchers.any(),Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(payeJson)))

      val result = await(TestEmploymentService.getPayAndTax("AA000000A", 2014, "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))
      result.json must be(payAndTaxJson)
    }

    "return not found and empty json object when failed to fetch payAndTaxURI from cache" in {
      lazy val payeJson = Json.obj()

      val payAndTaxJson  = Json.parse("""{}""".stripMargin)
      when(TestEmploymentService.getFromCache(Matchers.any(),Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(payeJson)))

      val result = await(TestEmploymentService.getPayAndTax("AA000000A", 2014, "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))
      result.status must be(NOT_FOUND)
      result.json must be(payAndTaxJson)
    }
  }
}
