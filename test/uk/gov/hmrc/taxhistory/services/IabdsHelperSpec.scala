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

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.api.Allowance
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.IabdsHelper


class IabdsHelperSpec extends PlaySpec with MockitoSugar with TestUtil{
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

val onlyIabdJson = """[{
                     |    "nino": "QQ000003",
                     |    "sequenceNumber": 201700055,
                     |    "taxYear": 2017,
                     |    "type": 35,
                     |    "source": 26,
                     |    "grossAmount": 100,
                     |    "receiptDate": null,
                     |    "captureDate": null,
                     |    "typeDescription": "Van Benefit",
                     |    "netAmount": null,
                     |    "employmentSequenceNumber": 1,
                     |    "costAmount": null,
                     |    "amountMadeGood": null,
                     |    "cashEquivalent": null,
                     |    "expensesIncurred": null,
                     |    "amountOfRelief": null
                     |  }]""".stripMargin



  val npsEmploymentResponse =  Json.parse(""" [{
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


  lazy val iabdsJsonResponse = loadFile("/json/nps/response/iabds.json")
  lazy val iabdList = iabdsJsonResponse.as[List[Iabd]]
  lazy val onlyIabdList = Json.parse(onlyIabdJson).as[List[Iabd]]

  lazy val iabdsTotalBenfitInKindJsonResponse = loadFile("/json/nps/response/iabdsTotalBIK.json")
  lazy val iabdsBenfitInKindJsonResponse = loadFile("/json/nps/response/iabdsBIK.json")


  "Iabds Helper" should {
    "correctly convert an iabd to an allowance model" in {
      val iabdsHelper = new IabdsHelper(iabdList)
      val allowances =  iabdsHelper.getAllowances()
      allowances.size mustBe 1
    }
    "Return an empty list of allowances when only iabd is present" in {
      val iabdsHelper = new IabdsHelper(onlyIabdList)

      val allowances =  iabdsHelper.getAllowances()
      allowances.size mustBe 0
    }

    "Return a filtered Iabds from  List of Nps Iabds" in {

      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val iabdsFiltered = iabdsHelper.getRawCompanyBenefits()
      iabdsFiltered.size mustBe 4
      iabdsFiltered.toString() contains  ("FlatRateJobExpenses") mustBe false
      iabdsFiltered.toString() contains  ("VanBenefit")  mustBe true
      iabdsFiltered.toString() contains  ("CarFuelBenefit")  mustBe true
    }

    "Return a matched iabds  from  List of employments" in {
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val employments = npsEmploymentResponse.as[List[NpsEmployment]]

      val matchedIabds = iabdsHelper.getMatchedCompanyBenefits(employments.head)
      matchedIabds.size mustBe 2
      matchedIabds.toString() contains  ("VanBenefit") mustBe true
      matchedIabds.toString() contains  ("CarFuelBenefit") mustBe true
    }

    "Get CompanyBenfits from Iabd data and ignore Benefit In Kind (type 28)" in {

      val iabds = iabdsBenfitInKindJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val companyBenefits=iabdsHelper.getCompanyBenefits()
      companyBenefits.size mustBe  7
      val companyBenefit1 = companyBenefits.head
      companyBenefit1.source mustBe Some(26)
      companyBenefit1.iabdType mustBe "UnKnown"
      companyBenefit1.amount mustBe 36795
    }

    "Get CompanyBenfits from Iabd data Benefit In Kind of type 28(Total Benefit In Kind)" in {

      val iabds = iabdsTotalBenfitInKindJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val companyBenefits=iabdsHelper.getCompanyBenefits()
      companyBenefits.size mustBe  2
    }

    "Total Benefit In Kind  from Iabds list should return true if There is only BIK which is type 28" in {
      val iabds = iabdsTotalBenfitInKindJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val bik=iabdsHelper.isTotalBenefitInKind()
      bik  mustBe  true
    }

    "Total Benefit In Kind  from Iabds list should return false if There is any BIK which is not type 28" in {
      val iabds = iabdsBenfitInKindJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val bik=iabdsHelper.isTotalBenefitInKind()
      bik  mustBe  false
    }

    "Return only Allowances from  List of Nps Iabds" in {
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val iabdsFiltered = iabdsHelper.getRawAllowances()
      iabdsFiltered.size mustBe 1
      iabdsFiltered.toString() contains  ("FlatRateJobExpenses") mustBe true
    }

  }
}
