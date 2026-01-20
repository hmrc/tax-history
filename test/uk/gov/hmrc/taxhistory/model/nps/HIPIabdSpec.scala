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
import play.api.libs.json.Json.fromJson
import play.api.libs.json.{JsError, JsNull, JsObject, JsValue, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.domain.TaxCode
import uk.gov.hmrc.http.GatewayTimeoutException
import uk.gov.hmrc.taxhistory.utils.TestUtil

import java.time.LocalDate
import scala.concurrent.Future

class HIPIabdSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {

  lazy val employmentsResponse: JsValue = loadFile("/json/nps/response/HIPiabds.json")
  private val grossAmount               = 200

  val iabdJsonResponse: JsObject = Json
    .parse(s"""{
         |        "nationalInsuranceNumber": "QQ00000AB",
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
    """.stripMargin)
    .as[JsObject]

  "HIPIabd Json" should {

    val iabd = iabdJsonResponse.as[HIPIabd]

    "transform Iabds Response Json correctly to Employment Model " in {
      iabd                  shouldBe a[HIPIabd]
      iabd.nino             shouldBe "QQ00000AB"
      iabd.`type`           shouldBe a[CompanyBenefits]
      iabd.`type`           shouldBe EmployerProvidedServices
      iabd.grossAmount      shouldBe Some(grossAmount)
      iabd.typeDescription  shouldBe Some("Total gift aid Payments")
      iabd.paymentFrequency shouldBe Some(6)
      iabd.startDate        shouldBe Some("23/02/2018")
    }
    "fail to parse HIPIabd when a required field is missing" in {
      val invalidJson = Json.parse("""{ "type": "foo (8)" }""")
      invalidJson.validate[HIPIabd] shouldBe a[JsError]
    }
    "handle paymentFrequency with a null value" in {
      val jsonWithNullPaymentFreq = iabdJsonResponse.as[JsObject] + ("paymentFrequency" -> JsNull)
      jsonWithNullPaymentFreq.as[HIPIabd].paymentFrequency shouldBe None
    }
    "return None for unknown paymentFrequency string" in {
      val json = iabdJsonResponse.as[JsObject] + ("paymentFrequency" -> Json.toJson("UNKNOWN"))
      json.as[HIPIabd].paymentFrequency shouldBe None
    }
    "handle source with a null value" in {
      val jsonWithNullPaymentFreq = iabdJsonResponse.as[JsObject] + ("source" -> JsNull)
      jsonWithNullPaymentFreq.as[HIPIabd].source shouldBe None
    }
    "return None for unknown source string" in {
      val json = iabdJsonResponse.as[JsObject] + ("source" -> Json.toJson("UNKNOWN"))
      json.as[HIPIabd].source shouldBe None
    }
    "handle startDate with a null value" in {
      val jsonWithNullStartDate = iabdJsonResponse.as[JsObject] + ("startDate" -> JsNull)
      jsonWithNullStartDate.as[HIPIabd].startDate shouldBe None
    }

    "List of Iabds Json" should {
      "transform List of Iabd" in {
        noException shouldBe thrownBy(employmentsResponse.as[HIPIabdList].getListOfIabd)
      }
    }

  }

  "HIPIabd" when {
    "serialise to json when an optional field is missing" in {
      val missingIabd = HIPIabd(
        nino = "QQ00000AB",
        `type` = EmployerProvidedServices,
        grossAmount = Some(grossAmount),
        typeDescription = Some("Total gift aid Payments"),
        source = Some(15),
        captureDate = Some("10/04/2017"),
        paymentFrequency = Some(6),
        startDate = Some("23/02/2018")
      )

      Json.toJson(missingIabd) shouldBe Json.obj(
        "nino"             -> "QQ00000AB",
        "type"             -> 8,
        "source"           -> Some(15),
        "grossAmount"      -> Some(grossAmount),
        "captureDate"      -> Some("10/04/2017"),
        "typeDescription"  -> Some("Total gift aid Payments"),
        "paymentFrequency" -> Some(6),
        "startDate"        -> Some("23/02/2018")
      )

    }
    "toStatePension is called" should {
      val testIabd = iabdJsonResponse.as[HIPIabd]

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

        "there is a paymentFrequency of 12" in {
          val paymentFrequency  = 12
          val iabdNoPaymentFreq =
            testIabd.copy(paymentFrequency = Some(paymentFrequency), startDate = Some("2018/04/23"))
          val statePension      = iabdNoPaymentFreq.toStatePension

          statePension.paymentFrequency shouldBe Some(paymentFrequency)
          statePension.startDate        shouldBe None
        }
      }
    }
  }

  "HIPIabdList" should {
    "returen empty list when getListOfIabd is called with empty HIPIabdList" in {
      Json.obj().as[HIPIabdList].getListOfIabd shouldBe List.empty
    }
  }
}
