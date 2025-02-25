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
    "read and write json successfully" in {
      EmploymentStatus.jsonReads.reads(EmploymentStatus.jsonWrites.writes(EmploymentStatus.Live))    shouldBe JsSuccess(
        Live
      )
      EmploymentStatus.jsonReads.reads(
        EmploymentStatus.jsonWrites.writes(EmploymentStatus.PermanentlyCeased)
      )                                                                                              shouldBe JsSuccess(
        PermanentlyCeased
      )
      EmploymentStatus.jsonReads.reads(EmploymentStatus.jsonWrites.writes(EmploymentStatus.Ceased))  shouldBe JsSuccess(
        Ceased
      )
      EmploymentStatus.jsonReads.reads(
        EmploymentStatus.jsonWrites.writes(EmploymentStatus.PotentiallyCeased)
      )                                                                                              shouldBe JsSuccess(PotentiallyCeased)
      EmploymentStatus.jsonReads.reads(EmploymentStatus.jsonWrites.writes(EmploymentStatus.Unknown)) shouldBe JsSuccess(
        Unknown
      )
    }
    "read the json correctly" in {
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "Live")) shouldBe JsSuccess(Live)
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "PotentiallyCeased")) shouldBe JsSuccess(
        PotentiallyCeased
      )
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "Ceased")) shouldBe JsSuccess(Ceased)
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "PermanentlyCeased")) shouldBe JsSuccess(
        PermanentlyCeased
      )
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> "Unknown")) shouldBe JsSuccess(
        Unknown
      )
    }

    "throw error on invalid data" in {
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> 10)) shouldBe JsError(
        List((JsPath \ "employmentStatus", List(JsonValidationError(List("Invalid EmploymentStatus")))))
      )
    }
  }
}
