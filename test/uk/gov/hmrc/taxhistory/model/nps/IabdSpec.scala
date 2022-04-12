/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.LocalDate
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import uk.gov.hmrc.taxhistory.utils.TestUtil


class IabdSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {

  lazy val employmentsResponse: JsValue = loadFile("/json/nps/response/iabds.json")
  private val grossAmount = 200

  val iabdJsonResponse: String =
    s"""{
       |        "nino": "QQ00000AB",
       |        "sequenceNumber": 201700001,
       |        "taxYear": 2017,
       |        "type": 8,
       |        "source": 15,
       |        "grossAmount": $grossAmount,
       |        "receiptDate": null,
       |        "captureDate": "10/04/2017",
       |        "typeDescription": "Total gift aid Payments",
       |        "netAmount": 100,
       |        "paymentFrequency": 1,
       |        "startDate": "23/02/2018"
       |
       |}
    """.stripMargin

  "Iabd Json" should {
    "transform Iabds Response Json correctly to Employment Model " in {
      val iabd = Json.parse(iabdJsonResponse).as[Iabd]
      iabd shouldBe a[Iabd]
      iabd.nino shouldBe "QQ00000AB"
      iabd.`type` shouldBe a[CompanyBenefits]
      iabd.`type` shouldBe EmployerProvidedServices
      iabd.grossAmount shouldBe Some(grossAmount)
      iabd.typeDescription shouldBe Some("Total gift aid Payments")
      iabd.paymentFrequency shouldBe Some(1)
      iabd.startDate shouldBe Some("23/02/2018")
    }

    "handle paymentFrequency with a null value" in {
      val jsonWithNullPaymentFreq = Json.parse(iabdJsonResponse).as[JsObject] + ("paymentFrequency" -> JsNull)
      jsonWithNullPaymentFreq.as[Iabd].paymentFrequency shouldBe None
    }

    "handle startDate with a null value" in {
      val jsonWithNullStartDate = Json.parse(iabdJsonResponse).as[JsObject] + ("startDate" -> JsNull)
      jsonWithNullStartDate.as[Iabd].startDate shouldBe None
    }

    "List of Iabds Json" should {
      "transform List of Iabd" in {
        noException shouldBe thrownBy(employmentsResponse.as[List[Iabd]])
      }
    }

  }

  "Iabd" when {
    "toStatePension is called" should {
      val testIabd = Json.parse(iabdJsonResponse).as[Iabd]

      "return StatePension with same grossAmount and typeDescription" in {
        val statePension = testIabd.toStatePension

        statePension.grossAmount shouldBe testIabd.grossAmount.get
        statePension.typeDescription shouldBe testIabd.typeDescription.get
      }

      "return StatePension's paymentFrequency and startDate" when {
        "there is no paymentFrequency" in {
          val iabdNoPaymentFreq = testIabd.copy(paymentFrequency = None, startDate = None)
          val statePension = iabdNoPaymentFreq.toStatePension

          statePension.paymentFrequency shouldBe None
          statePension.startDate shouldBe None
        }

        "there a paymentFrequency of 1" in {
          val iabdNoPaymentFreq = testIabd.copy(paymentFrequency = Some(1), startDate = Some("23/04/2018"))
          val statePension = iabdNoPaymentFreq.toStatePension

          statePension.paymentFrequency shouldBe Some(1)
          statePension.startDate shouldBe Some(LocalDate.parse("2018-04-23"))
        }

        "there is a paymentFrequency of 5" in {
          val iabdNoPaymentFreq = testIabd.copy(paymentFrequency = Some(5), startDate = Some("23/04/2018"))
          val statePension = iabdNoPaymentFreq.toStatePension

          statePension.paymentFrequency shouldBe Some(5)
          statePension.startDate shouldBe None
        }
      }
    }
  }
}