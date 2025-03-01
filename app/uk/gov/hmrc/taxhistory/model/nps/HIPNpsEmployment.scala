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
import uk.gov.hmrc.taxhistory.model.api.{Employment, EmploymentPaymentType}

case class HIPNpsEmployment(
  nino: String,
  sequenceNumber: Int,
  taxDistrictNumber: String,
  payeNumber: String,
  employerName: String,
  worksNumber: Option[String] = None,
  receivingJobSeekersAllowance: Boolean = false,
  otherIncomeSourceIndicator: Boolean = false,
  startDate: Option[LocalDate],
  endDate: Option[LocalDate] = None,
  receivingOccupationalPension: Boolean = false,
  employmentStatus: EmploymentStatus
) {

  def payeRef: String = taxDistrictNumber + "/" + payeNumber

  def toEmployment: Employment = {
    val employmentPaymentType =
      EmploymentPaymentType.paymentType(payeRef, receivingOccupationalPension, receivingJobSeekersAllowance)
    Employment(
      employerName = employerName,
      payeReference = payeRef,
      startDate = startDate,
      endDate = endDate,
      employmentPaymentType = employmentPaymentType,
      employmentStatus = employmentStatus,
      worksNumber = worksNumber.getOrElse("")
    )
  }
}

object HIPNpsEmployment {
//TODO: to be removed
  def toNpsEmployment(hipNpsEmployment: HIPNpsEmployment): NpsEmployment            = NpsEmployment(
    hipNpsEmployment.nino,
    hipNpsEmployment.sequenceNumber,
    hipNpsEmployment.taxDistrictNumber,
    hipNpsEmployment.payeNumber,
    hipNpsEmployment.employerName,
    hipNpsEmployment.worksNumber,
    hipNpsEmployment.receivingJobSeekersAllowance,
    hipNpsEmployment.otherIncomeSourceIndicator,
    hipNpsEmployment.startDate,
    hipNpsEmployment.endDate,
    hipNpsEmployment.receivingOccupationalPension,
    hipNpsEmployment.employmentStatus
  )
  def apply(nino: String)(hipNpsEmploymentWithoutNino: HIPNpsEmploymentWithoutNino) = new HIPNpsEmployment(
    nino,
    hipNpsEmploymentWithoutNino.sequenceNumber,
    hipNpsEmploymentWithoutNino.taxDistrictNumber,
    hipNpsEmploymentWithoutNino.payeNumber,
    hipNpsEmploymentWithoutNino.employerName,
    hipNpsEmploymentWithoutNino.worksNumber,
    hipNpsEmploymentWithoutNino.receivingJobSeekersAllowance,
    hipNpsEmploymentWithoutNino.otherIncomeSourceIndicator,
    hipNpsEmploymentWithoutNino.startDate,
    hipNpsEmploymentWithoutNino.endDate,
    hipNpsEmploymentWithoutNino.receivingOccupationalPension,
    hipNpsEmploymentWithoutNino.employmentStatus
  )
}
