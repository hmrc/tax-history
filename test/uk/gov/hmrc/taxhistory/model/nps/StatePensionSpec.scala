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

package uk.gov.hmrc.taxhistory.model.nps

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsError, JsValue, Json}
import uk.gov.hmrc.taxhistory.model.nps.{AllowanceOrDeduction, TaAllowance, TaDeduction}
import uk.gov.hmrc.taxhistory.utils.TestUtil

import java.time.LocalDate
import java.util.UUID

class StatePensionSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {

  lazy val json: JsValue = Json.parse("""
                                                 |{
                                                 |    "grossAmount": 1234,
                                                 |    "typeDescription": "State Pension",
                                                 |    "paymentFrequency": 1,
                                                 |    "startDate": "2018-01-23"
                                                 |}
        """.stripMargin)

  private val model: StatePension =
    StatePension(BigDecimal(1234), "State Pension", Some(1), Some(LocalDate.parse("2018-01-23")))

  "IncomeSource" when {
    "read from valid JSON" should {
      "produce the expected Tax Account model" in {
        json.as[StatePension] shouldBe model
      }

      "fail to read from json" when {
        "there is type mismatch" in {
          Json
            .obj(
              "grossAmount"      -> false,
              "typeDescription"  -> "State Pension",
              "paymentFrequency" -> 1,
              "startDate"        -> "2018-01-23"
            )
            .validate[StatePension] shouldBe a[JsError]
        }

        "empty json" in {
          Json.obj().validate[StatePension] shouldBe a[JsError]
        }
      }
    }

    "written to JSON" should {
      "produce the expected JSON" in {
        Json.toJson(model) shouldBe json
      }
    }
  }
}
