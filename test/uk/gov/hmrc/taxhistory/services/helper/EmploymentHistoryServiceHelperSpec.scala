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

package uk.gov.hmrc.taxhistory.services.helper

import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.taxhistory.model.api.{CompanyBenefit, Employment, PayAndTax, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentHistoryServiceHelper

class EmploymentHistoryServiceHelperSpec extends PlaySpec with MockitoSugar with TestUtil{

  private val mockAudit= mock[Audit]

  object EmploymentHistoryServiceHelperTest extends EmploymentHistoryServiceHelper{
    override def audit: Audit = mockAudit
  }

  val employment1 = Employment(payeReference = "1234",
                              startDate = new LocalDate("2016-10-20"),
                              employerName = "AnEmployerName")

  val employment2 = Employment(payeReference = "4321",
                               startDate = new LocalDate("2015-12-01"),
                               employerName = "AnotherEmployerName")
  val companyBenefit = CompanyBenefit(iabdType = "type",
                                      amount = BigDecimal(123.00))
  val payAndTax = PayAndTax(taxablePayTotal = Some(BigDecimal(2222.22)),
                            taxTotal = Some(BigDecimal(111.11)),
                            earlierYearUpdates=Nil)
  val payAsYouEarn1 = PayAsYouEarn(
    employments = List(employment1),
    allowances = Nil,
    benefits = Some(Map(employment1.employmentId.toString -> List(companyBenefit))),
    payAndTax = Some(Map(employment1.employmentId.toString -> payAndTax)))

  val payAsYouEarn2 = PayAsYouEarn(
    employments = List(employment2),
    allowances = Nil,
    benefits = Some(Map(employment2.employmentId.toString -> List(companyBenefit))),
    payAndTax = Some(Map(employment2.employmentId.toString -> payAndTax)))

  "EmploymentHistoryServiceHelper" should {
    "merge from two payAsYouEarn objects into one" in {
      val merged = EmploymentHistoryServiceHelperTest.mergeIntoSinglePayAsYouEarn(List(payAsYouEarn1,payAsYouEarn2))
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
      println(Json.toJson(merged))
    }
    "merge from one payAsYouEarn objects into one" in {
      val merged = EmploymentHistoryServiceHelperTest.mergeIntoSinglePayAsYouEarn(List(payAsYouEarn1))
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

      println(Json.toJson(merged))

    }
  }
}
