/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.JsValue
import uk.gov.hmrc.taxhistory.utils.TestUtil

class HipErrorSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {

  "HipErrors" must {
    "read from json" when {
      "receiving 503" in {
        lazy val errorJson: JsValue = loadFile("/json/nps/response/HipErrors/503.json")
        val hipError                = errorJson.as[HipErrors]
        hipError          shouldBe a[HipErrors]
        hipError.toString shouldBe "string : string"
      }

      "receiving 422" in {
        lazy val errorJson: JsValue = loadFile("/json/nps/response/HipErrors/422.json")
        val hipError                = errorJson.as[HipErrors]
        hipError          shouldBe a[HipErrors]
        hipError.toString shouldBe "string : string"
      }

      "receiving 403" in {
        lazy val errorJson: JsValue = loadFile("/json/nps/response/HipErrors/403.json")
        val hipError                = errorJson.as[HipErrors]
        hipError          shouldBe a[HipErrors]
        hipError.toString shouldBe "403.2 : Forbidden"
      }
      "receiving 400" in {
        lazy val errorJson: JsValue = loadFile("/json/nps/response/HipErrors/400.json")
        val hipError                = errorJson.as[HipErrors]
        hipError          shouldBe a[HipErrors]
        hipError.toString shouldBe "400.2 : HTTP message not readable , 400.1 : Constraint Violation - Invalid/Missing input parameter"
      }
    }
  }
}
