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
import uk.gov.hmrc.taxhistory.utils.TestUtil

import java.util.UUID

class TaxAcccountSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {

  lazy val accountJson: JsValue = Json.parse(
    """
      |{
      |    "taxAccountId": "3923afda-41ee-4226-bda5-e39cc4c82934",
      |    "outstandingDebtRestriction": 22.22,
      |    "underpaymentAmount": 11.11,
      |    "actualPUPCodedInCYPlusOneTaxYear": 33.33
      |}
    """.stripMargin
  )
  private val model: TaxAccount = TaxAccount(
    taxAccountId = UUID.fromString("3923afda-41ee-4226-bda5-e39cc4c82934"),
    outstandingDebtRestriction = Some(BigDecimal("22.22")),
    underpaymentAmount = Some(BigDecimal("11.11")),
    actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("33.33"))
  )

  "TaxAccount" when {
    "read from valid JSON" should {
      "produce the expected Tax Account model" in {
        accountJson.as[TaxAccount] shouldBe model
      }

      "fail to read from json" when {
        "there is type mismatch" in {
          Json
            .obj(
              "taxAccountId"                     -> 123456,
              "outstandingDebtRestriction"       -> 22.22,
              "underpaymentAmount"               -> 11.11,
              "actualPUPCodedInCYPlusOneTaxYear" -> 33.33
            )
            .validate[TaxAccount] shouldBe a[JsError]
        }
      }
    }

    "written to JSON" should {
      "produce the expected JSON" in {
        Json.toJson(model) shouldBe accountJson
      }
    }
  }
}
