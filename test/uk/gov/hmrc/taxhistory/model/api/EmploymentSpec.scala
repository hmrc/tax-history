/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.model.api

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

import java.util.UUID

import org.joda.time.LocalDate
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class EmploymentSpec extends TestUtil with UnitSpec {

  lazy val employmentJson: JsValue = loadFile("/json/model/api/employment.json")
  lazy val employmentNoEndDateJson: JsValue = loadFile("/json/model/api/employmentNoEndDate.json")
  lazy val employmentListJson: JsValue = loadFile("/json/model/api/employments.json")

  lazy val employment1 = Employment(
    employmentId = UUID.fromString("01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
    startDate = Some(new LocalDate("2016-01-21")),
    endDate = Some(new LocalDate("2017-01-01")),
    payeReference = "paye-1",
    employerName = "employer-1",
    employmentStatus = EmploymentStatus.Live,
    worksNumber = "00191048716"
  )

  lazy val employment2 = Employment(
    employmentId = UUID.fromString("019f5fee-d5e4-4f3e-9569-139b8ad81a87"),
    payeReference = "paye-2",
    employerName = "employer-2",
    startDate = Some(new LocalDate("2016-02-22")),
    employmentStatus = EmploymentStatus.Live,
    worksNumber = "00191048716"
  )

  lazy val employmentList = List(employment1, employment2)

  "Employment" should {

    "transform into Json from object correctly " in {
      Json.toJson(employment1) shouldBe employmentJson
    }
    "transform into object from json correctly " in {
      employmentJson.as[Employment] shouldBe employment1
    }
    "generate employmentId when none is supplied" in {
      val emp = Employment(payeReference = "paye-1",
        employerName = "employer-1",
        startDate = Some(new LocalDate("2016-01-21")),
        endDate = Some(new LocalDate("2017-01-01")),
        employmentStatus = EmploymentStatus.Live,
        worksNumber = "00191048716"
      )
      emp.employmentId.toString.nonEmpty shouldBe true
      emp.employmentId shouldNot be(employment1.employmentId)
    }
    "transform into Json from object list correctly " in {
      Json.toJson(employmentList) shouldBe employmentListJson
    }
    "transform into object list from json correctly " in {
      employmentListJson.as[List[Employment]] shouldBe employmentList
    }
    "allow omission of endDate in json" in {
      employmentNoEndDateJson.as[Employment] shouldBe employment2
    }
    "allow omission of startDate in json" in {
      val employmentNoStartDateJson = employmentNoEndDateJson.as[JsObject] - "startDate"
      val employmentNoStartDate = employment2.copy(startDate = None)

      employmentNoStartDateJson.as[Employment] shouldBe employmentNoStartDate
      Json.toJson(employmentNoStartDate) shouldBe employmentNoStartDateJson
    }
    "enrich employment with URIs" in {
      val taxYear = 2016
      val enrichedEmployment = employment1.enrichWithURIs(taxYear = taxYear)
      employment1.companyBenefitsURI shouldBe None
      employment1.employmentURI shouldBe None
      employment1.payAndTaxURI shouldBe None

      val employmentURI = s"/$taxYear/employments/${employment1.employmentId.toString}"

      enrichedEmployment.employmentURI shouldBe Some(employmentURI)
      enrichedEmployment.companyBenefitsURI shouldBe Some(employmentURI + "/company-benefits")
      enrichedEmployment.payAndTaxURI shouldBe Some(employmentURI + "/pay-and-tax")
    }

  }
}


