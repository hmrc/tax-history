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

import play.api.libs.json.JsValue
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class GetTaxAccountSpec extends TestUtil with UnitSpec {

  lazy val getTaxAcoountResponseURLDummy: JsValue = loadFile("/json/nps/response/GetTaxAccount.json")

  private val primaryEmploymentId = 12
  private val actualPupCodedInCYPlusOne = 240
  private val outStandingDebt = 145.75
  private val underPayment = 15423.29

  "GetTaxAccount" should {

    val taxAccount = getTaxAcoountResponseURLDummy.as[NpsTaxAccount]

    "transform Nps Get Tax Account Response Json correctly to NpsTaxAccount Model " in {
      taxAccount shouldBe a[NpsTaxAccount]
      taxAccount.getPrimaryEmploymentId shouldBe Some(primaryEmploymentId)
      taxAccount.getActualPupCodedInCYPlusOne shouldBe Some(actualPupCodedInCYPlusOne)
      taxAccount.getOutStandingDebt shouldBe Some(outStandingDebt)
      taxAccount.getUnderPayment shouldBe Some(underPayment)
    }
  }
}