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

import java.time.LocalDate
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}

class NpsEmploymentSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues with DateUtils {

  lazy val employmentsResponse: JsValue = loadFile("/json/nps/response/employments.json")

  val employmentResponse: String =
    """ {
      |    "nino": "AA000000",
      |    "sequenceNumber": 6,
      |    "employmentStatus": 1,
      |    "worksNumber": "00191048716",
      |    "taxDistrictNumber": "846",
      |    "payeNumber": "T2PP",
      |    "employerName": "Aldi",
      |    "receivingJobseekersAllowance" : true,
      |    "otherIncomeSourceIndicator": true,
      |    "receivingOccupationalPension": true,
      |    "startDate": "21/01/2015",
      |    "endDate": "08/01/2016"
      |    }
    """.stripMargin

  val startDate: LocalDate = LocalDate.of(YEAR_2015, JANUARY, DAY_21)
  val endDate: LocalDate   = LocalDate.of(YEAR_2016, JANUARY, DAY_8)

  "NpsEmployment" should {
    "transform Nps Employment Response Json correctly to Employment Model " in {
      val employment = Json.parse(employmentResponse).as[NpsEmployment]
      employment                              shouldBe a[NpsEmployment]
      employment.nino                         shouldBe "AA000000"
      employment.sequenceNumber               shouldBe 6
      employment.worksNumber                  shouldBe Some("00191048716")
      employment.taxDistrictNumber            shouldBe "846"
      employment.payeNumber                   shouldBe "T2PP"
      employment.employerName                 shouldBe "Aldi"
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
          |    "nino": "AA000000",
          |    "sequenceNumber": 6,
          |    "employmentStatus": 1,
          |    "worksNumber": "00191048716",
          |    "taxDistrictNumber": "46",
          |    "payeNumber": "T2PP",
          |    "employerName": "Aldi",
          |    "receivingJobseekersAllowance" : true,
          |    "otherIncomeSourceIndicator": true,
          |    "receivingOccupationalPension": true,
          |    "startDate": "21/01/2015",
          |    "endDate": "08/01/2016"
          |    }
        """.stripMargin

      val employment = Json.parse(employmentResponse).as[NpsEmployment]

      employment.taxDistrictNumber shouldBe "046"
    }

    "deserialise NPS Employment Response Json" when {
      "startDate is missing" in {
        val employmentNoStartDateJson: JsObject = Json.parse(employmentResponse).as[JsObject] - "startDate"
        val deserialised                        = employmentNoStartDateJson.as[NpsEmployment]
        deserialised.startDate shouldBe None
      }
      "startDate is null" in {
        val employmentNullStartDateJson: JsObject =
          Json.parse(employmentResponse).as[JsObject] + ("startDate" -> JsNull)
        val deserialised = employmentNullStartDateJson.as[NpsEmployment]
        deserialised.startDate shouldBe None
      }
    }

    "Multiple NpsEmployments Json" should {
      "transform List of NpsEmployment Model " in {
        noException shouldBe thrownBy(employmentsResponse.as[List[NpsEmployment]])
      }
    }
  }
}
