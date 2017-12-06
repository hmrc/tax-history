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

package uk.gov.hmrc.taxhistory.model.api

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

import java.util.UUID

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class PayeSpec extends TestUtil with UnitSpec {

  lazy val payeJson = loadFile("/json/model/api/paye.json")

  lazy val payeNoAllowancesJson = loadFile("/json/model/api/payeNoAllowances.json")

  lazy val payeNoCompanyBenefitsJson = loadFile("/json/model/api/payeNoCompanyBenefits.json")


  lazy val allowance1 = Allowance(allowanceId = UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),
    iabdType = "payeType",
    amount = BigDecimal(12.00))

  lazy val companyBenefit = CompanyBenefit(companyBenefitId = UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),
    iabdType = "companyBenefitType",
    amount = BigDecimal(12.00))

  lazy val employment1 =  Employment(
    employmentId = UUID.fromString("01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
    payeReference = "paye-1",
    employerName = "employer-1",
    startDate = new LocalDate("2016-01-21"),
    endDate = Some(new LocalDate("2017-01-01")),
    employmentStatus = EmploymentStatus.Live
  )
  lazy val employment2 = Employment(
    employmentId = UUID.fromString("019f5fee-d5e4-4f3e-9569-139b8ad81a87"),
    payeReference = "paye-2",
    employerName = "employer-2",
    startDate = new LocalDate("2016-02-22"),
    employmentStatus = EmploymentStatus.Live
  )
  lazy val payAndTax = PayAndTax(
    payAndTaxId = UUID.fromString("2e2abe0a-8c4f-49fc-bdd2-cc13054e7172"),
    taxablePayTotal = Some(BigDecimal(2222.22)),
    taxTotal = Some(BigDecimal(111.11)),
    paymentDate = Some(new LocalDate("2016-02-20")),
    earlierYearUpdates=Nil)

  lazy val employmentList = List(employment1,employment2)
  lazy val allowanceList = List(allowance1)
  lazy val companyBenefitList = List(companyBenefit)
  lazy val benefitsMap = Map("01318d7c-bcd9-47e2-8c38-551e7ccdfae3" -> companyBenefitList)
  lazy val payAndTaxMap = Map("01318d7c-bcd9-47e2-8c38-551e7ccdfae3" -> payAndTax)

 val paye = PayAsYouEarn(employmentList, allowanceList, Some(benefitsMap), payAndTax = Some(payAndTaxMap))
  val payeNoAllowances = PayAsYouEarn(employments=employmentList,benefits = Some(benefitsMap))
  val payeNoCompanyBenefits = PayAsYouEarn(employments=employmentList)

  "Paye" should {
    "transform into Json from object correctly " in {
      Json.toJson(paye) shouldBe payeJson
    }
    "transform into object from json correctly " in {
      payeJson.as[PayAsYouEarn] shouldBe paye
    }
    "transform into Json when there are no allowances" in {
      Json.toJson(payeNoAllowances) shouldBe payeNoAllowancesJson
    }
    "transform into object from json with no allowances" in {
      payeNoAllowancesJson.as[PayAsYouEarn] shouldBe payeNoAllowances
    }
    "transform into Json when there are no company benefits" in {
      Json.toJson(payeNoCompanyBenefits) shouldBe payeNoCompanyBenefitsJson
    }
  }
}


