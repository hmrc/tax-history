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

import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, _}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.api.EmploymentPaymentType._
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class EmploymentPaymentTypeSpec extends TestUtil with UnitSpec {

  case class TestObj(employmentPaymentType: Option[EmploymentPaymentType])
  implicit val testObjFormat = Json.format[TestObj]

  "EmploymentPaymentType" when {
    "(de)serialising an 'employmentPaymentType' field" should {

      "serialise to a missing field when the optional EmploymentPaymentType is empty" in {
        val noPaymentType = TestObj(employmentPaymentType = None)
        (toJson(noPaymentType) \ "employmentPaymentType") shouldBe a[JsUndefined]
      }
      "serialise to a json object when there is some EmploymentPaymentType present" in {
        val serialisedObj = Json.parse("""{ "employmentPaymentType": "OccupationalPension" }""")
        toJson(TestObj(employmentPaymentType = Some(OccupationalPension))) shouldBe serialisedObj
      }
      "deserialise from a missing field to a None" in {
        fromJson[TestObj](Json.parse("{}")).get shouldBe TestObj(None)
      }
      "deserialise from a field with valid value to some EmploymentPaymentType" in {
        val serialisedObj = Json.parse("""{ "employmentPaymentType": "OccupationalPension" }""")
        fromJson[TestObj](serialisedObj).get shouldBe TestObj(Some(OccupationalPension))
      }
      "throw an exception when deserialising from a field with invalid value" in {
        val serialisedObj = Json.parse("""{ "employmentPaymentType": "SomethingPeculiar" }""")
        fromJson[TestObj](serialisedObj) shouldBe a[JsError]
      }
    }
  }

  "calling paymentType" should {
    "return JobSeekersAllowance if 'receivingJobseekersAllowance' flag is true" in {
      paymentType("123/AB123", false, receivingJobSeekersAllowance = true) shouldBe Some(JobseekersAllowance)
    }
    "return OccupationalPension if 'receivingOccupationalPension' flag is true" in {
      paymentType("123/AB123", receivingOccupationalPension = true, false) shouldBe Some(OccupationalPension)
    }
    "return IncapacityBenefit if PAYE reference is '892/BA500'" in {
      paymentType("892/BA500", false, false) shouldBe Some(IncapacityBenefit)
    }
    "return EmploymentAndSupportAllowance if PAYE reference is '267/ESA500'" in {
      paymentType("267/ESA500", false, false) shouldBe Some(EmploymentAndSupportAllowance)
    }
    "return StatePensionLumpSum if PAYE reference is '267/LS500'" in {
      paymentType("267/LS500", false, false) shouldBe Some(StatePensionLumpSum)
    }
  }
}


