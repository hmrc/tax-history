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
import play.api.libs.json.{JsError, JsValue, Json}
import uk.gov.hmrc.taxhistory.model.nps.{AllowanceOrDeduction, TaAllowance, TaDeduction}
import uk.gov.hmrc.taxhistory.utils.TestUtil

import java.util.UUID

class IncomeSourceSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {

  lazy val json: JsValue          = Json.parse(
    """
      |{
      |  "employmentId": 1,
      |  "employmentType": 1,
      |  "actualPUPCodedInCYPlusOneTaxYear": 22.22,
      |  "deductions": [{
      |    "type": 15,
      |    "npsDescription": "balancing charge",
      |    "amount": 212,
      |    "sourceAmount": 212
      |  }],
      |  "allowances": [{
      |     "type": 11,
      |     "npsDescription": "personal allowance",
      |     "amount": 11000,
      |     "sourceAmount": 11000
      |  }],
      |  "taxCode": "1150L",
      |  "basisOperation": 1,
      |  "employmentTaxDistrictNumber": 1,
      |  "employmentPayeRef": "payeRef"
      |}
    """.stripMargin
  )
  private val model: IncomeSource = IncomeSource(
    1,
    1,
    Some(BigDecimal(22.22)),
    List(
      TaDeduction(
        `type` = 15,
        npsDescription = "balancing charge",
        amount = BigDecimal(212),
        sourceAmount = Some(212)
      )
    ),
    List(
      TaAllowance(
        `type` = 11,
        npsDescription = "personal allowance",
        amount = BigDecimal(11000),
        sourceAmount = Some(BigDecimal(11000))
      )
    ),
    "1150L",
    Some(1),
    1,
    "payeRef"
  )

  "IncomeSource" when {
    "read from valid JSON" should {
      "produce the expected Tax Account model" in {
        json.as[IncomeSource] shouldBe model
      }

      "fail to read from json" when {
        "there is type mismatch" in {
          Json
            .obj(
              "employmentId"                     -> "1",
              "employmentType"                   -> 1,
              "actualPUPCodedInCYPlusOneTaxYear" -> 22.22,
              "deductions"                       -> Json.arr(
                "type"           -> 15,
                "npsDescription" -> "balancing charge",
                "amount"         -> 212,
                "sourceAmount"   -> 212
              ),
              "allowances"                       -> Json.arr(
                "type"           -> 11,
                "npsDescription" -> "personal allowance",
                "amount"         -> 11000,
                "sourceAmount"   -> 11000
              ),
              "taxCode"                          -> "1150L",
              "basisOperation"                   -> 1,
              "employmentTaxDistrictNumber"      -> 1,
              "employmentPayeRef"                -> "payeRef"
            )
            .validate[IncomeSource] shouldBe a[JsError]
        }

        "empty json" in {
          Json.obj().validate[IncomeSource] shouldBe a[JsError]
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
