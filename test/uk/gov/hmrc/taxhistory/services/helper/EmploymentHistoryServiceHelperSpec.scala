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

package uk.gov.hmrc.taxhistory.services.helper

import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.api._
import uk.gov.hmrc.taxhistory.model.nps.{EmploymentStatus, Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentHistoryServiceHelper

class EmploymentHistoryServiceHelperSpec extends PlaySpec with MockitoSugar with TestUtil{

  private val mockAuditable = mock[Auditable]

  private val helper = new EmploymentHistoryServiceHelper(mockAuditable)

  lazy val npsEmploymentResponseWithTaxDistrictNumber =  Json.parse(""" [{
                                                                 |    "nino": "AA000000",
                                                                 |    "sequenceNumber": 6,
                                                                 |    "worksNumber": "6044041000000",
                                                                 |    "taxDistrictNumber": "0531",
                                                                 |    "payeNumber": "J4816",
                                                                 |    "employerName": "Aldi",
                                                                 |    "receivingJobseekersAllowance" : false,
                                                                 |    "otherIncomeSourceIndicator": false,
                                                                 |    "receivingOccupationalPension": false,
                                                                 |    "startDate": "21/01/2015",
                                                                 |    "employmentStatus":1
                                                                 |    }]
                                                               """.stripMargin)
  lazy val rtiEmploymentResponse = loadFile("/json/rti/response/dummyRti.json")
  lazy val iabdsJsonResponse = loadFile("/json/nps/response/iabds.json")
  lazy val taxAccountJsonResponse = loadFile("/json/nps/response/GetTaxAccount.json")
  val startDate = new LocalDate("2015-01-21")
  lazy val employment1 = Employment(payeReference = "1234",
    startDate = new LocalDate("2016-10-20"),
    employerName = "AnEmployerName",
    employmentStatus = EmploymentStatus.Live)

  lazy val employment2 = Employment(payeReference = "4321",
    startDate = new LocalDate("2015-12-01"),
    employerName = "AnotherEmployerName",
    employmentStatus = EmploymentStatus.Live)

  lazy val taxAccount = TaxAccount(
    underpaymentAmount = Some(BigDecimal(11.11)),
    outstandingDebtRestriction = Some(BigDecimal(22.22)),
    actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal(33.33)))

  lazy val companyBenefit = CompanyBenefit(iabdType = "type",
                                      amount = BigDecimal(123.00))
  lazy val payAndTax = PayAndTax(taxablePayTotal = Some(BigDecimal(2222.22)),
                            taxTotal = Some(BigDecimal(111.11)),
                            earlierYearUpdates=Nil)
  lazy val payAsYouEarn1 = PayAsYouEarn(
    employments = List(employment1),
    allowances = Nil,
    benefits = Some(Map(employment1.employmentId.toString -> List(companyBenefit))),
    payAndTax = Some(Map(employment1.employmentId.toString -> payAndTax)),
    taxAccount = None)

  lazy val payAsYouEarn2 = PayAsYouEarn(
    employments = List(employment2),
    allowances = Nil,
    benefits = Some(Map(employment2.employmentId.toString -> List(companyBenefit))),
    payAndTax = Some(Map(employment2.employmentId.toString -> payAndTax)),
    taxAccount = Some(taxAccount))

  "EmploymentHistoryServiceHelper" should {
    "merge from two payAsYouEarn objects into one" in {
      val merged = helper.mergeIntoSinglePayAsYouEarn(List(payAsYouEarn1,payAsYouEarn2), Nil, Some(taxAccount))
      merged.employments.size mustBe 2
      merged.employments must contain(employment1)
      merged.employments must contain(employment2)

      merged.allowances.size mustBe 0
      merged.allowances mustBe Nil

      merged.benefits.get.size mustBe 2
      val benefits1 = merged.benefits.get(employment1.employmentId.toString)
      benefits1.size mustBe 1
      benefits1 must contain(companyBenefit)

      val benefits2 = merged.benefits.get(employment2.employmentId.toString)
      benefits2.size mustBe 1
      benefits2 must contain(companyBenefit)

      merged.payAndTax.get.size mustBe 2
      val payAndTax1 = merged.payAndTax.get(employment1.employmentId.toString)
      payAndTax1 mustBe(payAndTax)

      val payAndTax2 = merged.payAndTax.get(employment2.employmentId.toString)
      payAndTax2 mustBe(payAndTax)

      val mergedTaxAccount = merged.taxAccount.get
      mergedTaxAccount.underpaymentAmount mustBe taxAccount.underpaymentAmount
      mergedTaxAccount.actualPUPCodedInCYPlusOneTaxYear mustBe taxAccount.actualPUPCodedInCYPlusOneTaxYear
      mergedTaxAccount.outstandingDebtRestriction mustBe taxAccount.outstandingDebtRestriction
    }
    "merge from one payAsYouEarn objects into one" in {
      val merged = helper.mergeIntoSinglePayAsYouEarn(List(payAsYouEarn1),Nil, None)
      merged.employments.size mustBe 1
      merged.employments must contain(employment1)

      merged.allowances.size mustBe 0
      merged.allowances mustBe Nil

      merged.benefits.get.size mustBe 1
      val benefits1 = merged.benefits.get(employment1.employmentId.toString)
      benefits1.size mustBe 1
      benefits1 must contain(companyBenefit)

      merged.payAndTax.get.size mustBe 1
      val payAndTax1 = merged.payAndTax.get(employment1.employmentId.toString)
      payAndTax1 mustBe(payAndTax)

      merged.taxAccount mustBe None

    }

    "Build pay as you earn using empty tax account" in {
      val taxAccount = NpsTaxAccount(Nil)
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val payAsYouEarn=helper.buildPayAsYouEarnList(None,None, npsEmployments.head)
      payAsYouEarn.taxAccount mustBe None
    }


    "Build employment1 from rti ,nps employment1 and Iabd data" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val taxAccount = taxAccountJsonResponse.as[NpsTaxAccount]

      val payAsYouEarn=helper.buildPayAsYouEarnList(Some(rtiData.employments),Some(iabds), npsEmployments.head)
      val employment = payAsYouEarn.employments.head
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      payAndTax.get.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.get.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.get.earlierYearUpdates.size mustBe 1
      payAsYouEarn.employments.head.startDate mustBe startDate
      payAsYouEarn.employments.head.endDate mustBe None
      val companyBenefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten
      companyBenefits.get.size mustBe  8
    }

    "Build employment1 when there is no  data for rti and Iabd" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val taxAccount = taxAccountJsonResponse.as[NpsTaxAccount]

      val payAsYouEarn=helper.buildPayAsYouEarnList(None,None, npsEmployments.head)
      val employment = payAsYouEarn.employments.head
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      payAndTax mustBe None
      val companyBenefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten
      companyBenefits mustBe  None

    }
    "Build employment1 when there is data for rti is Nil " in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val taxAccount = taxAccountJsonResponse.as[NpsTaxAccount]

      val payAsYouEarn=helper.buildPayAsYouEarnList(None,Some(iabds), npsEmployments.head)
      val employment = payAsYouEarn.employments.head
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      payAndTax mustBe None
      val companyBenefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten
      companyBenefits.get.size mustBe  8

    }

    "Build employment1 when there is data for Iabd is None or Null" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val taxAccount = taxAccountJsonResponse.as[NpsTaxAccount]

      val payAsYouEarn=helper.buildPayAsYouEarnList(Some(rtiData.employments),None, npsEmployments.head)
      val employment = payAsYouEarn.employments.head
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      payAndTax.get.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.get.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.get.earlierYearUpdates.size mustBe 1
      val companyBenefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten
      companyBenefits mustBe None

    }
    "Build employment1 when there is no  data for Iabd" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val taxAccount = taxAccountJsonResponse.as[NpsTaxAccount]

      val payAsYouEarn=helper.buildPayAsYouEarnList(Some(rtiData.employments),Some(Nil), npsEmployments.head)
      val employment = payAsYouEarn.employments.head
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      payAndTax.get.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.get.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.get.earlierYearUpdates.size mustBe 1
      val companyBenefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten
      companyBenefits mustBe  None
    }
  }
}
