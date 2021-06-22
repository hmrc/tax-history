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

import org.joda.time.LocalDate
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

import java.util.UUID

class PayAndTaxSpec extends TestUtil with WordSpecLike with Matchers with OptionValues {

  lazy val payAndTaxNoEyuJson: JsValue = loadFile("/json/model/api/payAndTaxNoEyu.json")
  lazy val payAndTaxWithEyuJson: JsValue = loadFile("/json/model/api/payAndTaxWithEyu.json")
  lazy val payAndTaxValuesNoneJson: JsValue = loadFile("/json/model/api/payAndTaxValuesNone.json")

  lazy val eyuList = List(EarlierYearUpdate(
    earlierYearUpdateId = UUID.fromString("e6926848-818b-4d01-baa1-02111eb0f514"),
    taxablePayEYU = BigDecimal(123.45),
    taxEYU = BigDecimal(67.89),
    receivedDate = new LocalDate("2015-05-29")))

  lazy val payAndTaxNoEyu: PayAndTax = PayAndTax(
    payAndTaxId = UUID.fromString("7407debb-5aa2-445d-8633-1875a2ebf559"),
    taxablePayTotal = Some(BigDecimal(76543.21)),
    taxablePayTotalIncludingEYU = Some(BigDecimal(76543.21)),
    taxTotal = Some(BigDecimal(6666.66)),
    taxTotalIncludingEYU = Some(BigDecimal(6666.66)),
    earlierYearUpdates = Nil,
    paymentDate = Some(new LocalDate("2016-02-20")))

  lazy val payAndTaxValuesNone: PayAndTax = PayAndTax(
    payAndTaxId = UUID.fromString("2dd8910e-95a4-4ede-b8af-977ca27b4a78"),
    taxablePayTotal = None,
    taxablePayTotalIncludingEYU = None,
    taxTotal = None,
    taxTotalIncludingEYU = None,
    earlierYearUpdates = Nil,
    paymentDate = None)

  lazy val payAndTaxWithEyu: PayAndTax = PayAndTax(
    payAndTaxId = UUID.fromString("bb1c1ea4-04d0-4285-a2e6-4ade1e57f12a"),
    taxablePayTotal = Some(BigDecimal(1234567.89)),
    taxablePayTotalIncludingEYU = Some(BigDecimal(1234691.34)),
    taxTotal = Some(BigDecimal(2222.22)),
    taxTotalIncludingEYU = Some(BigDecimal(2290.11)),
    earlierYearUpdates = eyuList,
    paymentDate = Some(new LocalDate("2016-02-20")))

  "PayAndTax" should {

    "transform into Json from object correctly without Eyu's" in {
      Json.toJson(payAndTaxNoEyu) shouldBe payAndTaxNoEyuJson
    }
    "transform into object from json correctly without Eyu's" in {
      payAndTaxNoEyuJson.as[PayAndTax] shouldBe payAndTaxNoEyu
    }

    "transform into Json from object correctly with Eyu's" in {
      Json.toJson(payAndTaxWithEyu) shouldBe payAndTaxWithEyuJson
    }
    "transform into object from json correctly with Eyu's" in {
      payAndTaxWithEyuJson.as[PayAndTax] shouldBe payAndTaxWithEyu
    }

    "transform into Json from object correctly without pay, tax or Eyu's" in {
      Json.toJson(payAndTaxValuesNone) shouldBe payAndTaxValuesNoneJson
    }
    "transform into object from json correctly without pay, tax or Eyu's" in {
      payAndTaxValuesNoneJson.as[PayAndTax] shouldBe payAndTaxValuesNone
    }

    "generate employmentId when none is supplied" in {
      val payAndTax = PayAndTax(
        taxablePayTotal = Some(BigDecimal(1212.12)),
        taxTotal = Some(BigDecimal(34.34)),
        earlierYearUpdates = Nil)
      payAndTax.payAndTaxId.toString.nonEmpty shouldBe true
      payAndTax.payAndTaxId shouldNot be(payAndTaxNoEyu.payAndTaxId)
    }
  }
}


