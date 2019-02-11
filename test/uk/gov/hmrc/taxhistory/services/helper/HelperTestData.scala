package uk.gov.hmrc.taxhistory.services.helper

import org.joda.time.LocalDate
import uk.gov.hmrc.tai.model.rti.{RtiEmployment, RtiPayment}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Ceased
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment

object HelperTestData {

  def rti(currentPayId:Option[String], seq: Int)  = RtiEmployment(
    payeRef = "U313",
    officeNumber = "951",
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

  def nps(worksNumber: Option[String], seq: Int) = NpsEmployment(
    payeNumber = "U313",
    taxDistrictNumber = "951",
    worksNumber = worksNumber,
    sequenceNumber = seq,
    nino = "AA000000A",
    employerName = "Employer 1",
    receivingJobSeekersAllowance = false,
    otherIncomeSourceIndicator = false,
    startDate = Some(LocalDate.parse("2014-04-28")),
    endDate = Some(LocalDate.parse("2014-07-28")),
    receivingOccupationalPension = false,
    employmentStatus = Ceased
  )
}
