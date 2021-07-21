/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model.rti

import com.github.nscala_time.time.Imports._
import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import uk.gov.hmrc.taxhistory.model.api.{EarlierYearUpdate, PayAndTax}
import uk.gov.hmrc.taxhistory.model.utils.JsonUtils

case class RtiData(nino: String,
                   employments: List[RtiEmployment])

case class RtiEmployment(sequenceNo: Int,
                         officeNumber: String,
                         payeRef: String,
                         currentPayId: Option[String] = None,
                         payments: List[RtiPayment],
                         earlierYearUpdates: List[RtiEarlierYearUpdate]) {

  def toPayAndTax: PayAndTax = {
    val eyus = earlierYearUpdates.map(_.toEarlierYearUpdate)
    val nonEmptyEyus = eyus.filter(eyu => eyu.taxablePayEYU != 0 && eyu.taxEYU != 0)
    val studentLoanEyus = eyus.filter(eyu => eyu.studentLoanEYU.isDefined)

    payments match {
      case Nil => PayAndTax(earlierYearUpdates = nonEmptyEyus, taxablePayTotalIncludingEYU = None, taxTotalIncludingEYU = None)
      case matchingPayments =>
        val payment = matchingPayments.max

        val taxablePayTotal = payment.taxablePayYTD
        val taxablePayTotalIncludingEYU = taxablePayTotal + nonEmptyEyus.map(_.taxablePayEYU).sum

        val taxTotal = payment.totalTaxYTD
        val taxTotalIncludingEYU = taxTotal + nonEmptyEyus.map(_.taxEYU).sum

        val studentLoan: Option[BigDecimal] = payment.studentLoansYTD
        val studentLoanIncludingEYU: Option[BigDecimal] = (studentLoan :: studentLoanEyus.map(_.studentLoanEYU)).flatten match {
          case Nil => None
          case values => Some(values.sum)
        }

        PayAndTax(taxablePayTotal = Some(taxablePayTotal),
          taxablePayTotalIncludingEYU = Some(taxablePayTotalIncludingEYU),
          taxTotal = Some(taxTotal),
          taxTotalIncludingEYU = Some(taxTotalIncludingEYU),
          studentLoan = studentLoan,
          studentLoanIncludingEYU = studentLoanIncludingEYU,
          paymentDate = Some(payment.paidOnDate),
          earlierYearUpdates = nonEmptyEyus)
    }
  }
}

case class RtiPayment(paidOnDate: LocalDate,
                      taxablePayYTD: BigDecimal,
                      totalTaxYTD: BigDecimal,
                      studentLoansYTD: Option[BigDecimal] = None) extends Ordered[RtiPayment] {
  def compare(that: RtiPayment): Int = this.paidOnDate compare that.paidOnDate
}

case class RtiEarlierYearUpdate(taxablePayDelta: BigDecimal,
                                totalTaxDelta: BigDecimal,
                                studentLoanRecoveredDelta: Option[BigDecimal] = None,
                                receivedDate: LocalDate) {

  def toEarlierYearUpdate: EarlierYearUpdate = {
    EarlierYearUpdate(
      taxablePayEYU = taxablePayDelta,
      taxEYU = totalTaxDelta,
      studentLoanEYU = studentLoanRecoveredDelta,
      receivedDate = receivedDate
    )
  }
}

object RtiPayment {
  implicit val reader: Reads[RtiPayment] = (js: JsValue) => {
    implicit val stringMapFormat: Format[Map[String, BigDecimal]] = JsonUtils.mapFormat[String, BigDecimal]("type", "amount")
    val mandatoryMonetaryAmountMap: Option[Map[String, BigDecimal]] =
      (js \ "mandatoryMonetaryAmount").asOpt[Map[String, BigDecimal]]

    val optionalMonetaryAmountMap: Option[Map[String, BigDecimal]] =
      (js \ "optionalMonetaryAmount").asOpt[Map[String, BigDecimal]]

    val taxablePay: BigDecimal = mandatoryMonetaryAmountMap.getOrElse(Map.empty).getOrElse("TaxablePayYTD", 0.0)
    val totalTax: BigDecimal = mandatoryMonetaryAmountMap.getOrElse(Map.empty).getOrElse("TotalTaxYTD", 0.0)
    val studentLoan: Option[BigDecimal] = optionalMonetaryAmountMap.getOrElse(Map.empty).get("StudentLoansYTD")

    JsSuccess(
      RtiPayment(
        paidOnDate = (js \ "pmtDate").as[LocalDate](JsonUtils.rtiDateFormat),
        taxablePayYTD = taxablePay,
        totalTaxYTD = totalTax,
        studentLoansYTD = studentLoan)
    )
  }

  implicit val writer: OWrites[RtiPayment] = Json.writes[RtiPayment]
}

object RtiEarlierYearUpdate {
  implicit val reader: Reads[RtiEarlierYearUpdate] = (js: JsValue) => {
    implicit val stringMapFormat: Format[Map[String, BigDecimal]] = JsonUtils.mapFormat[String, BigDecimal]("type", "amount")
    val optionalAdjustmentAmountMap: Option[Map[String, BigDecimal]] =
      (js \ "optionalAdjustmentAmount").asOpt[Map[String, BigDecimal]]

    val taxablePayDelta: BigDecimal = optionalAdjustmentAmountMap.getOrElse(Map.empty).getOrElse("TaxablePayDelta", 0.0)
    val totalTaxDelta: BigDecimal = optionalAdjustmentAmountMap.getOrElse(Map.empty).getOrElse("TotalTaxDelta", 0.0)
    val studentLoanRecoveredDelta: Option[BigDecimal] = optionalAdjustmentAmountMap.getOrElse(Map.empty).get("StudentLoanRecoveredDelta")
    val receivedDate = (js \ "rcvdDate").as[LocalDate](JsonUtils.rtiDateFormat)

    JsSuccess(
      RtiEarlierYearUpdate(taxablePayDelta = taxablePayDelta,
        totalTaxDelta = totalTaxDelta,
        studentLoanRecoveredDelta = studentLoanRecoveredDelta,
        receivedDate = receivedDate)
    )
  }
  implicit val writer: OWrites[RtiEarlierYearUpdate] = Json.writes[RtiEarlierYearUpdate]
}

object RtiEmployment {
  implicit val reader: Reads[RtiEmployment] = (js: JsValue) => {
    for {
      sequenceNo <- (js \ "sequenceNumber").validate[Int]
      officeNumber <- (js \ "empRefs" \ "officeNo").validate[String]
      payeRef <- (js \ "empRefs" \ "payeRef").validate[String]
      currentPayId <- (js \ "currentPayId").validateOpt[String]
      payments <- (js \ "payments" \ "inYear").validateOpt[List[RtiPayment]]
      earlierYearUpdates <- JsSuccess((js \ "payments" \ "eyu").asOpt[List[RtiEarlierYearUpdate]])
    } yield {
      RtiEmployment(
        sequenceNo = sequenceNo,
        payeRef = payeRef,
        officeNumber = officeNumber,
        currentPayId = currentPayId,
        payments = payments.getOrElse(List.empty),
        earlierYearUpdates = earlierYearUpdates.getOrElse(Nil)
      )
    }
  }
  implicit val writer: OWrites[RtiEmployment] = Json.writes[RtiEmployment]
}

object RtiData {
  implicit val reader: Reads[RtiData] = (js: JsValue) => {
    for {
      nino <- (js \ "request" \ "nino").validate[String]
      employments <- (js \ "individual" \ "employments" \ "employment").validate[List[RtiEmployment]]
    } yield {
      RtiData(nino = nino, employments = employments)
    }
  }
  implicit val writer: OWrites[RtiData] = Json.writes[RtiData]
}
