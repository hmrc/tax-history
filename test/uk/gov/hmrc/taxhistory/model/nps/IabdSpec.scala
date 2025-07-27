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
import play.api.libs.json.{JsError, JsNull, JsObject, JsResultException, JsValue, Json}
import uk.gov.hmrc.taxhistory.utils.TestUtil

import java.time.LocalDate

class IabdSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {

  lazy val employmentsResponse: JsValue = loadFile("/json/nps/response/HIPiabds.json")
  private val grossAmount               = 200

  val iabdJsonResponse: String =
    s"""{
       |        "nationalInsuranceNumber": "QQ00000AB",
       |        "iabdSequenceNumber": 201700001,
       |        "taxYear": 2017,
       |        "type": "Total gift aid Payments (8)",
       |        "source": "FPS(RTI)",
       |        "grossAmount": $grossAmount,
       |        "captureDate": "2017-04-10",
       |        "netAmount": 100,
       |        "paymentFrequency": "4 WEEKLY",
       |        "startDate": "2018-02-23"
       |
       |}
    """.stripMargin
  "HIPIabd Json" should {
    "transform Iabds Response Json correctly to Employment Model " in {
      val iabd = Json.parse(iabdJsonResponse).as[Iabd]
      iabd                  shouldBe a[Iabd]
      iabd.nino             shouldBe "QQ00000AB"
      iabd.`type`           shouldBe a[CompanyBenefits]
      iabd.`type`           shouldBe EmployerProvidedServices
      iabd.grossAmount      shouldBe Some(grossAmount)
      iabd.typeDescription  shouldBe Some("Total gift aid Payments")
      iabd.paymentFrequency shouldBe Some(6)
      iabd.startDate        shouldBe Some("23/02/2018")
    }
    "fail to parse HIPIabd when required field nino is missing" in {
      val invalidJson = Json.parse("""{ "type": "foo (8)" }""")
      invalidJson.validate[Iabd] shouldBe a[JsError]
    }
    "fail to parse HIPIabd when required field type is missing" in {
      val invalidJson = Json.parse("""{ "nino": "133njkws" }""")
      assertThrows[JsResultException] {
        invalidJson.validate[Iabd]
      }
    }
    "handle paymentFrequency with a null value" in {
      val jsonWithNullPaymentFreq = Json.parse(iabdJsonResponse).as[JsObject] + ("paymentFrequency" -> JsNull)
      jsonWithNullPaymentFreq.as[Iabd].paymentFrequency shouldBe None
    }
    "return None for unknown paymentFrequency string" in {
      val json = Json.parse(iabdJsonResponse).as[JsObject] + ("paymentFrequency" -> Json.toJson("UNKNOWN"))
      json.as[Iabd].paymentFrequency shouldBe None
    }
    "handle source with a null value" in {
      val jsonWithNullPaymentFreq = Json.parse(iabdJsonResponse).as[JsObject] + ("source" -> JsNull)
      jsonWithNullPaymentFreq.as[Iabd].source shouldBe None
    }
    "return None for unknown source string" in {
      val json = Json.parse(iabdJsonResponse).as[JsObject] + ("source" -> Json.toJson("UNKNOWN"))
      json.as[Iabd].source shouldBe None
    }
    "handle startDate with a null value" in {
      val jsonWithNullStartDate = Json.parse(iabdJsonResponse).as[JsObject] + ("startDate" -> JsNull)
      jsonWithNullStartDate.as[Iabd].startDate shouldBe None
    }
    "List of Iabds Json" should {
      "transform List of Iabd" in {
        noException shouldBe thrownBy(employmentsResponse.as[IabdList].getListOfIabd)
      }
    }

  }

  "HIPIabd" when {
    "toStatePension is called" should {
      val testIabd = Json.parse(iabdJsonResponse).as[Iabd]

      "return StatePension with same grossAmount and typeDescription" in {
        val statePension = testIabd.toStatePension

        statePension.grossAmount     shouldBe testIabd.grossAmount.get
        statePension.typeDescription shouldBe testIabd.typeDescription.get
      }

      "return StatePension's paymentFrequency and startDate" when {
        "there is no paymentFrequency" in {
          val iabdNoPaymentFreq = testIabd.copy(paymentFrequency = None, startDate = None)
          val statePension      = iabdNoPaymentFreq.toStatePension

          statePension.paymentFrequency shouldBe None
          statePension.startDate        shouldBe None
        }

        "there a paymentFrequency of 1" in {
          val paymentFrequency  = 1
          val iabdNoPaymentFreq =
            testIabd.copy(paymentFrequency = Some(paymentFrequency), startDate = Some("2018/04/23"))
          val statePension      = iabdNoPaymentFreq.toStatePension

          statePension.paymentFrequency shouldBe Some(1)
          statePension.startDate        shouldBe Some(LocalDate.parse("2018-04-23"))
        }

        "there is a paymentFrequency of 1" in {
          val paymentFrequency  = 1
          val iabdNoPaymentFreq =
            testIabd.copy(paymentFrequency = Some(paymentFrequency), startDate = Some("2018/04/23"))
          val statePension      = iabdNoPaymentFreq.toStatePension

          statePension.paymentFrequency shouldBe Some(paymentFrequency)
          statePension.startDate        shouldBe Some(LocalDate.parse("2018-04-23"))
        }
      }
    }
  }

  "HIPIabdList" should {
    "returen empty list when getListOfIabd is called with empty HIPIabdList" in {
      Json.obj().as[IabdList].getListOfIabd shouldBe List.empty
    }
  }
}
