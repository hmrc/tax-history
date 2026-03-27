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
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.{Ceased, Live, PermanentlyCeased, PotentiallyCeased, Unknown}

class EmploymentStatusSpec extends AnyWordSpec with Matchers with OptionValues {

  "EmploymentStatus" must {
    "read the json correctly" in {
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "Live"))               shouldBe JsSuccess(Live)
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "Potentially Ceased")) shouldBe JsSuccess(
        PotentiallyCeased
      )
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "Ceased"))             shouldBe JsSuccess(Ceased)
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "Unknown"))            shouldBe JsSuccess(Unknown)
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "Permanently Ceased")) shouldBe JsSuccess(
        PermanentlyCeased
      )
    }

    "write the json correctly" in {
      EmploymentStatus.jsonWrites.writes(Live)              shouldBe Json.obj("employmentStatus" -> 1)
      EmploymentStatus.jsonWrites.writes(PotentiallyCeased) shouldBe Json.obj("employmentStatus" -> 2)
      EmploymentStatus.jsonWrites.writes(Ceased)            shouldBe Json.obj("employmentStatus" -> 3)
      EmploymentStatus.jsonWrites.writes(Unknown)           shouldBe Json.obj("employmentStatus" -> 99)
      EmploymentStatus.jsonWrites.writes(PermanentlyCeased) shouldBe Json.obj("employmentStatus" -> 6)
    }

    "throw error on invalid data" in {
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "Invalid")) shouldBe JsError(
        List((JsPath \ "employmentStatus", List(JsonValidationError(List("Invalid EmploymentStatus")))))
      )
    }
  }
}
