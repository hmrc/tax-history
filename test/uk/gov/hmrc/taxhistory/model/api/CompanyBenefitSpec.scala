/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.libs.json.Json.{fromJson, toJson}
import play.api.libs.json.{JsArray, JsDefined, JsError, JsValue, Json}
import uk.gov.hmrc.taxhistory.utils.TestUtil
import uk.gov.hmrc.time.TaxYear

import java.util.UUID

class CompanyBenefitSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {

  lazy val companyBenefitJson: JsValue     = Json.parse("""
      |{
      |  "companyBenefitId":"c9923a63-4208-4e03-926d-7c7c88adc7ee",
      |  "iabdType":"companyBenefitType",
      |  "amount":12,
      |  "source":1,
      |  "captureDate":"05/04/2022",
      |  "taxYear" : {
      |     "startYear":2022
      |   },
      |  "isForecastBenefit":true
      |}
    """.stripMargin)
  lazy val companyBenefitListJson: JsArray = JsArray(Seq(companyBenefitJson))

  lazy val companyBenefit: CompanyBenefit = CompanyBenefit(
    companyBenefitId = UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),
    iabdType = "companyBenefitType",
    amount = BigDecimal(12.00),
    source = Some(1),
    captureDate = Some("05/04/2022"),
    TaxYear(2022)
  )

  lazy val companyBenefitList: List[CompanyBenefit] = List(companyBenefit)

  "CompanyBenefit" should {

    "correctly transform into Json from object" in {
      Json.toJson(companyBenefit) shouldBe companyBenefitJson
    }

    "correctly transform into object from Json " in {
      companyBenefitJson.as[CompanyBenefit] shouldBe companyBenefit
    }

    "generate companyBenefitId when source = None is supplied" in {
      val comBenefit = CompanyBenefit(
        iabdType = "otherCompanyBenefitType",
        amount = BigDecimal(10.00),
        source = None,
        captureDate = Some("05/04/2022"),
        taxYear = TaxYear(2022)
      )
      comBenefit.companyBenefitId.toString.nonEmpty shouldBe true
      comBenefit.companyBenefitId                  shouldNot be(companyBenefit.companyBenefitId)
    }

    "transform into Json from object list correctly" in {
      Json.toJson(companyBenefitList) shouldBe companyBenefitListJson
    }

    "transform into object list from json correctly" in {
      companyBenefitListJson.as[List[CompanyBenefit]] shouldBe companyBenefitList
    }

    "fail to read from json" when {
      "there is type mismatch" in {
        Json
          .obj(
            "iabdType"    -> 12345678,
            "amount"      -> BigDecimal(10.00),
            "source"      -> None,
            "captureDate" -> Some("05/04/2022"),
            "taxYear"     -> 2022
          )
          .validate[CompanyBenefit] shouldBe a[JsError]
      }

      "a required field is missing" in {
        Json
          .obj(
            "iabdType"    -> "otherCompanyBenefitType",
            "amount"      -> BigDecimal(10.00),
            "source"      -> None,
            "captureDate" -> Some("05/04/2022")
          )
          .validate[CompanyBenefit] shouldBe a[JsError]
      }

      "empty json" in {
        Json.obj().validate[CompanyBenefit] shouldBe a[JsError]
      }
    }

    ".isForecastBenefit()" when {

      "the captureDate is == 5th April of a given tax year (start of tax year)" should {

        "return false, since the benefit is P11d status" in {

          val companyBenefit = CompanyBenefit(
            iabdType = "",
            amount = BigDecimal(0),
            source = None,
            captureDate = Some("5/4/2022"),
            taxYear = TaxYear(2022)
          )
          companyBenefit.isForecastBenefit shouldBe true
        }
      }

      "the captureDate is after 5th April of the current tax year" should {

        "return false, since the benefit is P11d status" in {

          val companyBenefit = CompanyBenefit(
            iabdType = "",
            amount = BigDecimal(0),
            source = None,
            captureDate = Some("6/04/2022"),
            taxYear = TaxYear(2022)
          )
          companyBenefit.isForecastBenefit shouldBe false
        }
      }

      "the captureDate is before 5th April of a given tax year" should {

        "return true the benefit should be a forecast" in {

          val companyBenefit = CompanyBenefit(
            iabdType = "",
            amount = BigDecimal(0),
            source = None,
            captureDate = Some("04/4/2022"),
            taxYear = TaxYear(2022)
          )
          companyBenefit.isForecastBenefit shouldBe true
        }
      }
    }
  }
}
