/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.taxhistory.utils.TestUtil


class IndividualTaxYearSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {

  lazy val indiviualTaxYearJson: JsValue = loadFile("/json/model/api/individual-tax-year.json")
  lazy val indiviualTaxYearListJson: JsValue = loadFile("/json/model/api/individual-tax-years.json")

  lazy val indivTaxYear1: IndividualTaxYear = IndividualTaxYear(year = 2016, allowancesURI = "/2016/allowances", employmentsURI = "/2016/employments", taxAccountURI = "/2016/tax-account")
  lazy val indivTaxYear2: IndividualTaxYear = IndividualTaxYear(year = 2015, allowancesURI = "/2015/allowances", employmentsURI = "/2015/employments", taxAccountURI = "/2015/tax-account")

  lazy val taxYearList = List(indivTaxYear1, indivTaxYear2)

  "IndividualTaxYear" should {
    "transform into Json from object correctly " in {
      Json.toJson(indivTaxYear1) shouldBe indiviualTaxYearJson
    }
    "transform into object from json correctly " in {
      indiviualTaxYearJson.as[IndividualTaxYear] shouldBe indivTaxYear1
    }
    "transform into Json from object list correctly " in {
      Json.toJson(taxYearList) shouldBe indiviualTaxYearListJson
    }
    "transform into object list from json correctly " in {
      indiviualTaxYearListJson.as[List[IndividualTaxYear]] shouldBe taxYearList
    }
  }
}