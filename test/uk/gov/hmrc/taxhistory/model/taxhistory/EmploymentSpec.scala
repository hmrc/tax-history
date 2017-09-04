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

package uk.gov.hmrc.taxhistory.model.taxhistory

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class EmploymentSpec extends TestUtil with UnitSpec {

  lazy val employmentDetailsJson = loadFile("/json/taxhistory/employments.json")

  lazy val employments = List(
    Employment(
      payeReference = "paye-1",
      employerName = "employer-1",
      startDate = new LocalDate("2016-01-21"),
      endDate = Some(new LocalDate("2017-01-01")),
      taxablePayTotal = Some(BigDecimal.valueOf(123.12)),
      taxablePayEYU = Some(BigDecimal.valueOf(-12.12)),
      taxTotal = Some(BigDecimal.valueOf(14.14)),
      taxEYU = Some(BigDecimal.valueOf(-1.14))),
    Employment(
      payeReference = "paye-2",
      employerName = "employer-2",
      startDate = new LocalDate("2016-01-02"),
      endDate = None,
      taxablePayTotal = Some(BigDecimal.valueOf(543.21)),
      taxablePayEYU = Some(BigDecimal.valueOf(-21.00)),
      taxTotal = Some(BigDecimal.valueOf(78.90)),
      taxEYU = Some(BigDecimal.valueOf(-4.56)))
    )

  "Employment List" should {

    "transform into Json from object list correctly " in {
      Json.toJson(employments) shouldBe employmentDetailsJson
    }
    "transform into object list from json correctly " in {
      employmentDetailsJson.as[List[Employment]] shouldBe employments
    }
  }
}

