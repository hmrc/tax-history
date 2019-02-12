/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class AllowanceSpec extends TestUtil with UnitSpec {

  lazy val allowanceJson: JsValue = loadFile("/json/model/api/allowance.json")
  lazy val allowanceListJson: JsValue = loadFile("/json/model/api/allowances.json")

  lazy val allowance1 = Allowance(allowanceId = UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),
                                  iabdType = "allowanceType",
                                  amount = BigDecimal(12.00))

  lazy val allowanceList = List(allowance1)

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
      allowance.allowanceId shouldNot be(allowance1.allowanceId)
    }
    "transform into Json from object list correctly " in {
      Json.toJson(allowanceList) shouldBe allowanceListJson
    }
    "transform into object list from json correctly " in {
      allowanceListJson.as[List[Allowance]] shouldBe allowanceList
    }
  }
}


