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

package uk.gov.hmrc.taxhistory.services.helper

import org.joda.time.LocalDate
import uk.gov.hmrc.tai.model.rti.{RtiEmployment, RtiPayment}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Ceased
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment

object HelperTestData {

  def rti(payeRef: String = "U313", officeNumber: String = "951", currentPayId:Option[String], seq: Int)  = RtiEmployment(
    payeRef = payeRef,
    officeNumber = officeNumber,
    currentPayId = currentPayId,
    sequenceNo = seq,
    payments = List(
      RtiPayment(
        paidOnDate = LocalDate.parse("2014-04-28"),
        taxablePayYTD = BigDecimal("760.00"),
        totalTaxYTD = BigDecimal("152.00"),
        studentLoansYTD = Some(BigDecimal("0.00"))
      ),
      RtiPayment(
        paidOnDate = LocalDate.parse("2014-05-28"),
        taxablePayYTD = BigDecimal("760.00"),
        totalTaxYTD = BigDecimal("152.00"),
        studentLoansYTD = Some(BigDecimal("0.00"))
      ),
      RtiPayment(
        paidOnDate = LocalDate.parse("2014-06-28"),
        taxablePayYTD = BigDecimal("1520.00"),
        totalTaxYTD = BigDecimal("304.00"),
        studentLoansYTD = None
      ),
      RtiPayment(
        paidOnDate = LocalDate.parse("2014-07-28"),
        taxablePayYTD = BigDecimal("1520.00"),
        totalTaxYTD = BigDecimal("304.00"),
        studentLoansYTD = None
      )
    ),
    earlierYearUpdates = Nil
  )

  def nps(payeNumber: String = "U313", taxDistrictNumber: String = "951", worksNumber: Option[String], seq: Int) = NpsEmployment(
    payeNumber = payeNumber,
    taxDistrictNumber = taxDistrictNumber,
    worksNumber = worksNumber,
    sequenceNumber = seq,
    nino = "AA000000A",
    employerName = s"Employer $payeNumber",
    receivingJobSeekersAllowance = false,
    otherIncomeSourceIndicator = false,
    startDate = Some(LocalDate.parse("2014-04-28")),
    endDate = Some(LocalDate.parse("2014-07-28")),
    receivingOccupationalPension = false,
    employmentStatus = Ceased
  )
}
