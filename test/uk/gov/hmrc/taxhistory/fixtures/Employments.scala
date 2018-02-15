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

package uk.gov.hmrc.taxhistory.fixtures

import java.util.UUID

import uk.gov.hmrc.taxhistory.model.api.Employment
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.time.TaxYear

trait Employments {

  val testWorksNumber = "00191048716"

  val liveOngoingEmployment = Employment(UUID.randomUUID(), TaxYear.current.starts, None, "Nothing", "An Employer",
    None, None, None, false, EmploymentStatus.Live, testWorksNumber)

  val liveStartYearEmployment = Employment(UUID.randomUUID(), TaxYear.current.starts, Some(TaxYear.current.starts.plusDays(10)), "Nothing", "An Employer",
    None, None, None, false, EmploymentStatus.Live, testWorksNumber)

  val liveMidYearEmployment = Employment(UUID.randomUUID(), TaxYear.current.starts.plusDays(10) , Some(TaxYear.current.finishes.minusDays(10)), "Nothing", "An Employer",
    None, None, None, false, EmploymentStatus.Live, testWorksNumber)

  val liveEndYearEmployment = Employment(UUID.randomUUID(), TaxYear.current.finishes.minusDays(10) , Some(TaxYear.current.finishes), "Nothing", "An Employer",
    None, None, None, false, EmploymentStatus.Live, testWorksNumber)



}
