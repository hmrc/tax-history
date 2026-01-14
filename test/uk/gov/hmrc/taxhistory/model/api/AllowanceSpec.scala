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

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsError, JsValue, Json}
import uk.gov.hmrc.taxhistory.utils.TestUtil

import java.util.UUID

class AllowanceSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {

  lazy val allowanceJson: JsValue     = loadFile("/json/model/api/allowance.json")
  lazy val allowanceListJson: JsValue = loadFile("/json/model/api/allowances.json")

  lazy val allowance1: Allowance = Allowance(
    allowanceId = UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),
    iabdType = "allowanceType",
    amount = BigDecimal(12.00)
  )

  lazy val allowanceList: List[Allowance] = List(allowance1)

  "Allowance" should {
    "transform into Json from object correctly " in {
      Json.toJson(allowance1) shouldBe allowanceJson
    }
    "transform into object from json correctly " in {
      allowanceJson.as[Allowance] shouldBe allowance1
    }
    "generate allowanceId when none is supplied" in {
      val allowance = Allowance(iabdType = "otherAllowanceType", amount = BigDecimal(10.00))
      allowance.allowanceId.toString.nonEmpty shouldBe true
      allowance.allowanceId                  shouldNot be(allowance1.allowanceId)
    }
    "transform into Json from object list correctly " in {
      Json.toJson(allowanceList) shouldBe allowanceListJson
    }
    "transform into object list from json correctly " in {
      allowanceListJson.as[List[Allowance]] shouldBe allowanceList
    }

    "fail to read from json" when {
      "there is type mismatch" in {
        Json
          .obj(
            "allowanceId"   -> UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),
            "iabdType"      -> 12345678,
            "allowancetype" -> BigDecimal(12.00)
          )
          .validate[Allowance] shouldBe a[JsError]
      }

      "a required field is missing" in {
        Json
          .obj(
            "allowanceId" -> UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),
            "iabdType"    -> "allowanceType"
          )
          .validate[Allowance] shouldBe a[JsError]
      }

      "empty json" in {
        Json.obj().validate[Allowance] shouldBe a[JsError]
      }
    }
  }
}
