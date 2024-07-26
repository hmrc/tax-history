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

package uk.gov.hmrc.taxhistory.model.rti

import java.time.LocalDate
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.taxhistory.fixtures.RtiEmployments
import uk.gov.hmrc.taxhistory.utils.TestUtil

class RtiEmploymentSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues with RtiEmployments {
  private val testEmploymentTaxablePayYTD   = BigDecimal(1234)
  private val testEmploymentTotalTaxYTD     = BigDecimal(5678)
  private val testEmploymentStudentLoansYTD = BigDecimal(333.33)
  private val testRtiPayment                = RtiPayment(
    paidOnDate = LocalDate.parse("2018-01-01"),
    taxablePayYTD = testEmploymentTaxablePayYTD,
    totalTaxYTD = testEmploymentTotalTaxYTD,
    studentLoansYTD = Some(testEmploymentStudentLoansYTD)
  )
  private val testEmployment                = RtiEmployment(
    sequenceNo = 1,
    officeNumber = "1",
    payeRef = "1",
    currentPayId = Some("1"),
    payments = List(testRtiPayment),
    earlierYearUpdates = Nil
  )

  "RtiEmployment" when {
    "toPayAndTax is called" should {
      "convert itself to PayAndTax" in {
        val rtiData   = rtiEmploymentResponse.as[RtiData]
        val payAndTax = rtiData.employments.head.toPayAndTax
        payAndTax.taxablePayTotal         should be(Some(rtiERTaxablePayTotal))
        payAndTax.taxTotal                should be(Some(rtiERTaxTotal))
        payAndTax.studentLoan             should be(Some(testEmploymentStudentLoansYTD))
        payAndTax.earlierYearUpdates.size should be(1)
      }

      "calculate taxablePayTotalIncludingEYU" when {
        "there are EYUs of type 'TaxablePayDelta', it sums them all together with the 'taxablePayTotal'" in {
          val payAndTax = testEmployment
            .copy(
              earlierYearUpdates = List(
                RtiEarlierYearUpdate(
                  taxablePayDelta = BigDecimal(101),
                  totalTaxDelta = BigDecimal(307),
                  receivedDate = LocalDate.parse("2018-01-01")
                ),
                RtiEarlierYearUpdate(
                  taxablePayDelta = BigDecimal(-103),
                  totalTaxDelta = BigDecimal(-311),
                  receivedDate = LocalDate.parse("2018-01-01")
                )
              )
            )
            .toPayAndTax
          payAndTax.taxablePayTotalIncludingEYU shouldBe Some(testEmploymentTaxablePayYTD + 101 - 103)
        }
        "there are no EYUs of type 'TaxablePayDelta', it just has the same value as 'taxablePayTotal" in {
          val payAndTax = testEmployment
            .copy(
              earlierYearUpdates = List(
                RtiEarlierYearUpdate(
                  taxablePayDelta = BigDecimal(0),
                  totalTaxDelta = BigDecimal(307),
                  receivedDate = LocalDate.parse("2018-01-01")
                )
              )
            )
            .toPayAndTax
          payAndTax.taxablePayTotalIncludingEYU shouldBe Some(testEmploymentTaxablePayYTD)
        }
        "there are no EYUs, it just has the same value as 'taxablePayTotal'" in {
          val payAndTax = testEmployment.copy(earlierYearUpdates = Nil).toPayAndTax
          payAndTax.taxablePayTotalIncludingEYU shouldBe Some(testEmploymentTaxablePayYTD)
        }
        "there are no payments, it is an empty Option" in {
          val payAndTax = testEmployment.copy(payments = Nil).toPayAndTax
          payAndTax.taxablePayTotalIncludingEYU shouldBe None
        }
      }

      "calculate taxTotalIncludingEYU" when {
        "there are EYUs of type 'TotalTaxDelta', it sums them all together with the 'taxTotal'" in {
          val payAndTax = testEmployment
            .copy(
              earlierYearUpdates = List(
                RtiEarlierYearUpdate(
                  taxablePayDelta = BigDecimal(101),
                  totalTaxDelta = BigDecimal(307),
                  receivedDate = LocalDate.parse("2018-01-01")
                ),
                RtiEarlierYearUpdate(
                  taxablePayDelta = BigDecimal(-103),
                  totalTaxDelta = BigDecimal(-311),
                  receivedDate = LocalDate.parse("2018-01-01")
                )
              )
            )
            .toPayAndTax
          payAndTax.taxTotalIncludingEYU shouldBe Some(testEmploymentTotalTaxYTD + 307 - 311)
        }
        "there are no EYUs of type 'TotalTaxDelta', it just has the same value as 'taxTotal" in {
          val payAndTax = testEmployment
            .copy(
              earlierYearUpdates = List(
                RtiEarlierYearUpdate(
                  taxablePayDelta = BigDecimal(101),
                  totalTaxDelta = BigDecimal(0),
                  receivedDate = LocalDate.parse("2018-01-01")
                )
              )
            )
            .toPayAndTax
          payAndTax.taxTotalIncludingEYU shouldBe Some(testEmploymentTotalTaxYTD)
        }
        "there are no EYUs, it just has the same value as 'taxTotal'" in {
          val payAndTax = testEmployment.copy(earlierYearUpdates = Nil).toPayAndTax
          payAndTax.taxTotalIncludingEYU shouldBe Some(testEmploymentTotalTaxYTD)
        }
        "there are no payments, it is an empty Option" in {
          val payAndTax = testEmployment.copy(payments = Nil).toPayAndTax
          payAndTax.taxTotalIncludingEYU shouldBe None
        }
      }

      "calculate studentLoansIncludingEYU" when {
        val rtiEYU = RtiEarlierYearUpdate(
          studentLoanRecoveredDelta = Some(101),
          taxablePayDelta = BigDecimal("0"),
          totalTaxDelta = BigDecimal("0"),
          receivedDate = LocalDate.parse("2018-01-01")
        )

        "there are EYUs of type 'StudentLoanRecoveredDelta', it sums them all together with the 'studentLoan'" in {
          val payAndTax = testEmployment
            .copy(
              earlierYearUpdates = List(
                rtiEYU.copy(studentLoanRecoveredDelta = Some(101)),
                rtiEYU.copy(studentLoanRecoveredDelta = Some(-103))
              )
            )
            .toPayAndTax
          payAndTax.studentLoanIncludingEYU shouldBe Some(testEmploymentStudentLoansYTD + 101 - 103)
        }
        "there are EYUs of type 'StudentLoanRecoveredDelta', but there's no 'studentLoan', it just sums the EYUs without any 'studentLoan'" in {
          val payAndTax = testEmployment
            .copy(
              earlierYearUpdates = List(
                rtiEYU.copy(studentLoanRecoveredDelta = Some(101)),
                rtiEYU.copy(studentLoanRecoveredDelta = Some(-103))
              ),
              payments = List(testRtiPayment.copy(studentLoansYTD = None))
            )
            .toPayAndTax

          payAndTax.studentLoan             shouldBe None
          payAndTax.studentLoanIncludingEYU shouldBe Some(101 - 103)
        }
        "there are no EYUs of type 'StudentLoanRecoveredDelta', it just has the same value as 'studentLoan" in {
          val payAndTax = testEmployment
            .copy(
              earlierYearUpdates = List(
                RtiEarlierYearUpdate(
                  studentLoanRecoveredDelta = None,
                  taxablePayDelta = BigDecimal("1"),
                  totalTaxDelta = BigDecimal("2"),
                  receivedDate = LocalDate.parse("2018-01-01")
                )
              )
            )
            .toPayAndTax
          payAndTax.studentLoanIncludingEYU shouldBe payAndTax.studentLoan
          payAndTax.studentLoanIncludingEYU shouldBe Some(testEmploymentStudentLoansYTD)
        }
        "there are no EYUs, it just has the same value as 'studentLoan'" in {
          val payAndTax = testEmployment.copy(earlierYearUpdates = Nil).toPayAndTax
          payAndTax.studentLoanIncludingEYU shouldBe payAndTax.studentLoan
          payAndTax.studentLoanIncludingEYU shouldBe Some(testEmploymentStudentLoansYTD)
        }
        "there are no payments, it is an empty Option" in {
          val payAndTax = testEmployment.copy(payments = Nil).toPayAndTax
          payAndTax.studentLoanIncludingEYU shouldBe None
        }
      }
    }
  }
}
