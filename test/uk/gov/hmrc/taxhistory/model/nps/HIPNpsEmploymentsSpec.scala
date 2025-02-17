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
import uk.gov.hmrc.taxhistory.model.nps.HIPNpsEmployments.toListOfHIPNpsEmployment
import uk.gov.hmrc.taxhistory.utils.TestUtil

class HIPNpsEmploymentsSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues {
  lazy val employmentsResponse: JsValue = loadFile("/json/nps/response/hipEmployments.json")
  "HIPNpsEmployments" should {
    "transform Nps Employment Response Json correctly to Employment Model " in {
      val employments = employmentsResponse.as[HIPNpsEmployments]
      employments                                     shouldBe a[HIPNpsEmployments]
      employments.nino                                shouldBe "RN000001A"
      employments.hipNpsEmploymentsWithoutNino.length shouldBe 2
      toListOfHIPNpsEmployment(employments)           shouldBe a[List[HIPNpsEmployment]]
    }
  }
}
