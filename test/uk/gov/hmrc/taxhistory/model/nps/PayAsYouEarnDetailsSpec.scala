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

package uk.gov.hmrc.taxhistory.model.nps

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.taxhistory.{Allowance, CompanyBenefit, Employment, PayAsYouEarnDetails}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil


class PayAsYouEarnDetailsSpec extends TestUtil with UnitSpec {


  val companyBenefit = CompanyBenefit("Medical Insurance",22.0)
  val allowance = Allowance("FRE",22.0)

  val startDate = new LocalDate("2016-02-21")

  val employment = Employment("employername","dddd", startDate, None, Some(22.00),Some(222.33),Some(22.00),None)

  val payAsYouEarnDetails = PayAsYouEarnDetails(List(employment))

  "PayAsYouEarnDetails with company benefits" should {
    "correctly transformed to the Json " in {
      val payAsYouEarnDetailsJson = Json.toJson[PayAsYouEarnDetails](payAsYouEarnDetails)
      payAsYouEarnDetailsJson.toString() should include("employername")
      (((payAsYouEarnDetailsJson \ "employments") (0) \ "companyBenefits") (0)).toString should include("[]")
      payAsYouEarnDetailsJson.toString() should include("allowances")
      println( payAsYouEarnDetailsJson.toString())
      ((payAsYouEarnDetailsJson \ "allowances")(0)).toString should include("[]")


    }
  }
    "PayAsYouEarnDetails with company benefits and allowance" should {
      "correctly transformed to the Json " in {
        val withEmployment = employment.copy(companyBenefits=List(companyBenefit))

        val payAsYouEarnDetails = PayAsYouEarnDetails(List(withEmployment),List(allowance))


        val payAsYouEarnDetailsJson = Json.toJson[PayAsYouEarnDetails](payAsYouEarnDetails)
        payAsYouEarnDetailsJson.toString() should include("employername")
        (((payAsYouEarnDetailsJson \ "employments")(0) \ "companyBenefits")(0)).toString should include("""typeDescription":"Medical Insurance""")
        payAsYouEarnDetailsJson.toString() should include("allowances")
        ((payAsYouEarnDetailsJson \ "allowances")(0)).toString should include(""""typeDescription":"FRE"""")

      }



  }



}
