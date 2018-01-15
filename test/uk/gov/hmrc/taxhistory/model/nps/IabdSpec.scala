/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.utils.TestUtil


class IabdSpec extends TestUtil with UnitSpec {

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
      |        "netAmount": 100
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

    }

    "List of Iabds Json" should {
      "transform List of Iabd" in {
        val iabds = employmentsResponse.as[List[Iabd]]
        iabds shouldBe a[List[Iabd]]
      }
    }

  }
}