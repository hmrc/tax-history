/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class NpsEmploymentSpec extends TestUtil with UnitSpec {

  lazy val employmentsResponse = loadFile("/json/nps/response/employments.json")

  val employmentResponse = """ {
                             |    "nino": "AA000000",
                             |    "sequenceNumber": 6,
                             |    "worksNumber": "00191048716",
                             |    "taxDistrictNumber": "846",
                             |    "payeNumber": "T2PP",
                             |    "employerName": "Aldi",
                             |    "receivingJobseekersAllowance" : true,
                             |    "startDate": "21/01/2015",
                             |    "endDate": "08/01/2016"
                             |    }
                             """.stripMargin

  val startDate = new LocalDate("2015-01-21")
  val endDate = new LocalDate("2016-01-08")

  "NpsEmployment" should {
    "transform Nps Employment Response Json correctly to Employment Model " in {
      val employment = Json.parse(employmentResponse).as[NpsEmployment]
      employment shouldBe a[NpsEmployment]
      employment.nino  shouldBe "AA000000"
      employment.sequenceNumber  shouldBe 6
      employment.worksNumber  shouldBe Some("00191048716")
      employment.taxDistrictNumber  shouldBe "846"
      employment.payeNumber  shouldBe "T2PP"
      employment.employerName  shouldBe "Aldi"
      employment.receivingJobSeekersAllowance shouldBe true
      employment.startDate shouldBe startDate
      employment.endDate shouldBe Some(endDate)
    }

    "Multiple NpsEmployments Json" should {
      "transform List of NpsEmployment Model " in {
        val npsEmployments = employmentsResponse.as[List[NpsEmployment]]
        npsEmployments shouldBe a [List[NpsEmployment]]
      }
    }
  }
}
