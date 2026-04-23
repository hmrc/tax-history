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

import java.time.LocalDate
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsError, JsValue, Json}
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}

import java.util.UUID

class PayAndTaxSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues with DateUtils {

  lazy val payAndTaxNoEyuJson: JsValue      = loadFile("/json/model/api/payAndTaxNoEyu.json")
  lazy val payAndTaxWithEyuJson: JsValue    = loadFile("/json/model/api/payAndTaxWithEyu.json")
  lazy val payAndTaxValuesNoneJson: JsValue = loadFile("/json/model/api/payAndTaxValuesNone.json")

  lazy val payAndTaxNoEyu: PayAndTax = PayAndTax(
    payAndTaxId = UUID.fromString("7407debb-5aa2-445d-8633-1875a2ebf559"),
    taxablePayTotal = Some(BigDecimal(76543.21)),
    taxTotal = Some(BigDecimal(6666.66)),
    paymentDate = Some(LocalDate.of(YEAR_2016, FEBRUARY, DAY_20))
  )

  lazy val payAndTaxValuesNone: PayAndTax = PayAndTax(
    payAndTaxId = UUID.fromString("2dd8910e-95a4-4ede-b8af-977ca27b4a78"),
    taxablePayTotal = None,
    taxTotal = None,
    paymentDate = None
  )

  lazy val payAndTaxWithEyu: PayAndTax = PayAndTax(
    payAndTaxId = UUID.fromString("bb1c1ea4-04d0-4285-a2e6-4ade1e57f12a"),
    taxablePayTotal = Some(BigDecimal(1234567.89)),
    taxTotal = Some(BigDecimal(2222.22)),
    paymentDate = Some(LocalDate.of(YEAR_2016, FEBRUARY, DAY_20))
  )

  "PayAndTax" should {

    "transform into Json from object correctly" in {
      Json.toJson(payAndTaxNoEyu) shouldBe payAndTaxNoEyuJson
    }
    "transform into object from json correctly" in {
      payAndTaxNoEyuJson.as[PayAndTax] shouldBe payAndTaxNoEyu
    }

    "transform into Json from object when previously containing EYUs" in {
      Json.toJson(payAndTaxWithEyu) shouldBe payAndTaxWithEyuJson
    }
    "transform into object from json when previously containing EYUs" in {
      payAndTaxWithEyuJson.as[PayAndTax] shouldBe payAndTaxWithEyu
    }

    "transform into Json from object correctly without pay or tax" in {
      Json.toJson(payAndTaxValuesNone) shouldBe payAndTaxValuesNoneJson
    }
    "transform into object from json correctly without pay or tax" in {
      payAndTaxValuesNoneJson.as[PayAndTax] shouldBe payAndTaxValuesNone
    }

    "fail to read from json when payAndTaxId has wrong type" in {
      Json.obj("payAndTaxId" -> "not-a-valid-uuid").validate[PayAndTax] shouldBe a[JsError]
    }

    "generate employmentId when none is supplied" in {
      val payAndTax = PayAndTax(
        taxablePayTotal = Some(BigDecimal(1212.12)),
        taxTotal = Some(BigDecimal(34.34))
      )
      payAndTax.payAndTaxId.toString.nonEmpty shouldBe true
      payAndTax.payAndTaxId                  shouldNot be(payAndTaxNoEyu.payAndTaxId)
    }
  }
}
