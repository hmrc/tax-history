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
import play.api.libs.json.Json
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
    payments = List(testRtiPayment)
  )

  "RtiEmployment" when {

    "instantiated without optional currentPayId" should {
      "default currentPayId to None" in {
        val emp = RtiEmployment(sequenceNo = 1, officeNumber = "1", payeRef = "1", payments = Nil)
        emp.currentPayId shouldBe None
      }
    }

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(testEmployment) shouldBe Json.obj(
          "sequenceNo"   -> 1,
          "officeNumber" -> "1",
          "payeRef"      -> "1",
          "currentPayId" -> Some("1"),
          "payments"     -> List(testRtiPayment)
        )
      }

      "an optional field is missing" in {
        Json.toJson(testEmployment.copy(currentPayId = None)) shouldBe Json.obj(
          "sequenceNo"   -> 1,
          "officeNumber" -> "1",
          "payeRef"      -> "1",
          "payments"     -> List(testRtiPayment)
        )
      }
    }

    "toPayAndTax is called" should {
      "convert itself to PayAndTax" in {
        val rtiData   = rtiEmploymentResponse.as[RtiData]
        val payAndTax = rtiData.employments.head.toPayAndTax
        payAndTax.taxablePayTotal should be(Some(rtiERTaxablePayTotal))
        payAndTax.taxTotal        should be(Some(rtiERTaxTotal))
        payAndTax.studentLoan     should be(Some(testEmploymentStudentLoansYTD))
      }

      "convert itself to PayAndTax (using STUDENT_LOAN_YTD variation)" in {
        val rtiData   = rtiEmploymentResponseWithStudentLoanYTDVariation.as[RtiData]
        val payAndTax = rtiData.employments.head.toPayAndTax
        payAndTax.taxablePayTotal should be(Some(rtiERTaxablePayTotal))
        payAndTax.taxTotal        should be(Some(rtiERTaxTotal))
        payAndTax.studentLoan     should be(Some(testEmploymentStudentLoansYTD))
      }

      "return PayAndTax with base FPS values only when there are no payments" in {
        val payAndTax = testEmployment.copy(payments = Nil).toPayAndTax
        payAndTax.taxablePayTotal shouldBe None
        payAndTax.taxTotal        shouldBe None
        payAndTax.studentLoan     shouldBe None
      }
    }
  }
}
