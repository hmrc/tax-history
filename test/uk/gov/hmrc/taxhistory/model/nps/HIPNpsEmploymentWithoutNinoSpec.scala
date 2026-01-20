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
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}

import java.time.LocalDate

class HIPNpsEmploymentWithoutNinoSpec
    extends TestUtil
    with AnyWordSpecLike
    with Matchers
    with OptionValues
    with DateUtils {
  lazy val employmentResponse: String   = """ {
  |  "payeSchemeOperatorName": "Scheme Y",
   | "employerReference": "987/Z654321",
    |"employmentSequenceNumber": 321,
  |  "employmentStatus": "Live",
   | "worksNumber": "12345",
    |"otherIncomeSource": true,
  |  "jobSeekersAllowance": true,
   | "activeOccupationalPension": true,
    |"startDate": "2015-01-21",
    |"endDate": "2017-06-08"
  }""".stripMargin
  lazy val employmentsResponse: JsValue = loadFile("/json/nps/response/hipEmploymentsWithoutNINO.json")
  val startDate: LocalDate              = LocalDate.of(YEAR_2015, JANUARY, DAY_21)
  val endDate: LocalDate                = LocalDate.of(YEAR_2017, JUNE, DAY_8)

  "HIPNpsEmploymentWithoutNino" should {
    "transform Nps Employment Response Json correctly to Employment Model " in {
      val employment = Json.parse(employmentResponse).as[HIPNpsEmploymentWithoutNino]
      employment                              shouldBe a[HIPNpsEmploymentWithoutNino]
      employment.sequenceNumber               shouldBe 321
      employment.worksNumber                  shouldBe Some("12345")
      employment.taxDistrictNumber            shouldBe "987"
      employment.payeNumber                   shouldBe "Z654321"
      employment.employerName                 shouldBe "Scheme Y"
      employment.receivingJobSeekersAllowance shouldBe true
      employment.receivingOccupationalPension shouldBe true
      employment.otherIncomeSourceIndicator   shouldBe true
      employment.startDate                    shouldBe Some(startDate)
      employment.endDate                      shouldBe Some(endDate)
      employment.employmentStatus             shouldBe EmploymentStatus.Live
    }

    "create an NpsEmployment with a 3 digit taxDistrictNumber when the Json response has a 2 digit taxDistrictNumber" in {
      val employmentResponse: String =
        """ {
          |  "payeSchemeOperatorName": "Scheme Y",
          | "employerReference": "98/Z654321",
          |"employmentSequenceNumber": 321,
          |  "employmentStatus": "Live",
          | "worksNumber": "12345",
          |"otherIncomeSource": true,
          |  "jobSeekersAllowance": true,
          | "activeOccupationalPension": true,
          |"startDate": "2015-01-21",
          |"endDate": "2017-06-08"
          |}
        """.stripMargin
      val employment                 = Json.parse(employmentResponse).as[HIPNpsEmploymentWithoutNino]

      employment.taxDistrictNumber shouldBe "098"
    }

    "create an Nps Employent Json when an optional field is missing" in {
      val missingOptionalField = HIPNpsEmploymentWithoutNino(
        sequenceNumber = 1,
        taxDistrictNumber = "46",
        payeNumber = "T2PP",
        employerName = "Aldi",
        worksNumber = Some("00191048716"),
        receivingJobSeekersAllowance = true,
        otherIncomeSourceIndicator = true,
        receivingOccupationalPension = true,
        startDate = Some(LocalDate.of(YEAR_2015, JANUARY, DAY_21)),
        employmentStatus = Live
      )

      Json.toJson(missingOptionalField) shouldBe Json.obj(
        "sequenceNumber"               -> 1,
        "taxDistrictNumber"            -> "46",
        "payeNumber"                   -> "T2PP",
        "employerName"                 -> "Aldi",
        "worksNumber"                  -> "00191048716",
        "receivingJobSeekersAllowance" -> true,
        "otherIncomeSourceIndicator"   -> true,
        "startDate"                    -> LocalDate.of(YEAR_2015, JANUARY, DAY_21),
        "receivingOccupationalPension" -> true,
        "employmentStatus"             -> Json.obj("employmentStatus" -> 1)
      )
    }

    "deserialise HIPNpsEmploymentWithoutNino Response Json" when {
      "startDate is missing" in {
        val employmentNoStartDateJson: JsObject = Json.parse(employmentResponse).as[JsObject] - "startDate"
        val deserialised                        = employmentNoStartDateJson.as[HIPNpsEmploymentWithoutNino]
        deserialised.startDate shouldBe None
      }
      "startDate is null" in {
        val employmentNullStartDateJson: JsObject =
          Json.parse(employmentResponse).as[JsObject] + ("startDate" -> JsNull)
        val deserialised = employmentNullStartDateJson.as[HIPNpsEmploymentWithoutNino]
        deserialised.startDate shouldBe None
      }
    }

    "Multiple HIPNpsEmploymentWithoutNino Json" should {
      "transform List of NpsEmployment Model " in {
        noException shouldBe thrownBy(employmentsResponse.as[List[HIPNpsEmploymentWithoutNino]])
      }
    }
  }
}
