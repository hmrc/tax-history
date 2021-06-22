/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.libs.json.{JsArray, JsBoolean, JsDefined, Json}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

import java.util.UUID

class CompanyBenefitSpec extends TestUtil with WordSpecLike with Matchers with OptionValues {

  lazy val companyBenefitJson = Json.parse(
    """
      |{
      |  "companyBenefitId":"c9923a63-4208-4e03-926d-7c7c88adc7ee",
      |  "iabdType":"companyBenefitType",
      |  "amount":12,
      |  "source" : 1,
      |  "isForecastBenefit" : true
      |}
    """.stripMargin)
  lazy val companyBenefitListJson = JsArray(Seq(companyBenefitJson))


  lazy val companyBenefit = CompanyBenefit(
    companyBenefitId = UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),
    iabdType = "companyBenefitType",
    amount = BigDecimal(12.00),
    source = Some(1)
  )

  lazy val companyBenefitList = List(companyBenefit)

  "CompanyBenefit" should {
    "transform into Json from object correctly " in {
      Json.toJson(companyBenefit) shouldBe companyBenefitJson
    }
    "transform into object from json correctly " in {
      companyBenefitJson.as[CompanyBenefit] shouldBe companyBenefit
    }
    "generate companyBenefitId when none is supplied" in {
      val comBenefit = CompanyBenefit(iabdType = "otherCompanyBenefitType", amount = BigDecimal(10.00))
      comBenefit.companyBenefitId.toString.nonEmpty shouldBe true
      comBenefit.companyBenefitId shouldNot be(companyBenefit.companyBenefitId)
    }
    "transform into Json from object list correctly " in {
      Json.toJson(companyBenefitList) shouldBe companyBenefitListJson
    }
    "transform into object list from json correctly " in {
      companyBenefitListJson.as[List[CompanyBenefit]] shouldBe companyBenefitList
    }

    "transform into Json with an 'isForecastBenefit' field" in {
      val forecastSource = Some(1)
      val actualSource = Some(19)
      (Json.toJson(companyBenefit.copy(source = actualSource)) \ "isForecastBenefit") shouldBe JsDefined(JsBoolean(false))
      (Json.toJson(companyBenefit.copy(source = forecastSource)) \ "isForecastBenefit") shouldBe JsDefined(JsBoolean(true))
      (Json.toJson(companyBenefit.copy(source = None)) \ "isForecastBenefit") shouldBe JsDefined(JsBoolean(true))
    }

    "determine the 'isForecastBenefit' flag" when {
      val benefit = CompanyBenefit(iabdType = "", amount = BigDecimal(0), source = None)
      "source is 19 - P11D (ECS), then it is not forecast" in {
        benefit.copy(source = Some(19)).isForecastBenefit shouldBe false
      }
      "source is 21 - P11D (MANUAL), then it is not forecast" in {
        benefit.copy(source = Some(21)).isForecastBenefit shouldBe false
      }
      "source is 28 - P11D (Assessed P11D), then it is not forecast" in {
        benefit.copy(source = Some(28)).isForecastBenefit shouldBe false

      }
      "source is 29 - P11D (P11D/P9D), then it is not forecast" in {
        benefit.copy(source = Some(29)).isForecastBenefit shouldBe false

      }
      "source is some other number, then it is forecast" in {
        val p11dSources = Seq(19, 21, 28, 29)
        val forecastSources = (1 to 200).filterNot(p11dSources.contains)
        forecastSources.foreach{ forecastSource =>
          benefit.copy(source = Some(forecastSource)).isForecastBenefit shouldBe true
        }
      }
      "source is not present, then it is forecast" in {
        benefit.copy(source = None).isForecastBenefit shouldBe true
      }
    }
  }
}


