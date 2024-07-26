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

package uk.gov.hmrc.taxhistory.fixtures

import java.util.UUID

import uk.gov.hmrc.taxhistory.model.api.Employment
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.time.TaxYear

trait Employments {

  val testWorksNumber = "00191048716"

  private def templateEmployment        = Employment(
    employmentId = UUID.randomUUID(),
    startDate = Some(TaxYear.current.starts),
    endDate = None,
    payeReference = "Nothing",
    employerName = "An Employer",
    None,
    None,
    None,
    None,
    employmentStatus = EmploymentStatus.Live,
    testWorksNumber
  )
  val liveOngoingEmployment: Employment = templateEmployment

  val liveNoEndEmployment: Employment = templateEmployment.copy(startDate = Some(TaxYear.current.starts.plusDays(30)))

  val liveStartYearEmployment: Employment = templateEmployment.copy(endDate = Some(TaxYear.current.starts.plusDays(10)))

  val liveMidYearEmployment: Employment = templateEmployment.copy(
    startDate = Some(TaxYear.current.starts.plusDays(40)),
    endDate = Some(TaxYear.current.finishes.minusDays(10))
  )

  val liveEndYearEmployment: Employment = templateEmployment.copy(
    startDate = Some(TaxYear.current.finishes.minusDays(10)),
    endDate = Some(TaxYear.current.finishes)
  )

  val ceasedBeforeStartEmployment: Employment = templateEmployment.copy(
    startDate = Some(TaxYear.current.previous.starts.plusDays(5)),
    endDate = Some(TaxYear.current.starts.plusDays(30)),
    employmentStatus = EmploymentStatus.Ceased
  )

  val ceasedNoEndEmployment: Employment = templateEmployment.copy(
    startDate = Some(TaxYear.current.starts.plusDays(90)),
    employmentStatus = EmploymentStatus.Ceased
  )

  val ceasedAfterEndEmployment: Employment = templateEmployment.copy(
    startDate = Some(TaxYear.current.starts.plusDays(60)),
    endDate = Some(TaxYear.current.next.starts.plusDays(30)),
    employmentStatus = EmploymentStatus.Ceased
  )

  val potentiallyCeasedEmployment: Employment = templateEmployment.copy(
    startDate = Some(TaxYear.current.starts.plusDays(90)),
    employmentStatus = EmploymentStatus.PotentiallyCeased
  )

}
