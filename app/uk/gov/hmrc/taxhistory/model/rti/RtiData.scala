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

import play.api.libs.json.*
import uk.gov.hmrc.taxhistory.model.api.PayAndTax
import uk.gov.hmrc.taxhistory.model.utils.JsonUtils

import java.time.LocalDate
import scala.math.Ordered.orderingToOrdered

case class RtiData(nino: String, employments: List[RtiEmployment])

case class RtiEmployment(
  sequenceNo: Int,
  officeNumber: String,
  payeRef: String,
  currentPayId: Option[String] = None,
  payments: List[RtiPayment]
) {

  def toPayAndTax: PayAndTax =
    payments match {
      case Nil              =>
        PayAndTax()
      case matchingPayments =>
        val payment = matchingPayments.max

        PayAndTax(
          taxablePayTotal = Some(payment.taxablePayYTD),
          taxTotal = Some(payment.totalTaxYTD),
          studentLoan = payment.studentLoansYTD,
          paymentDate = Some(payment.paidOnDate)
        )
    }
}

case class RtiPayment(
  paidOnDate: LocalDate,
  taxablePayYTD: BigDecimal,
  totalTaxYTD: BigDecimal,
  studentLoansYTD: Option[BigDecimal] = None
) extends Ordered[RtiPayment] {
  def compare(that: RtiPayment): Int = this.paidOnDate compare that.paidOnDate
}

object RtiPayment {
  given reader: Reads[RtiPayment] = (js: JsValue) => {
    given stringMapFormat: Format[Map[String, BigDecimal]] =
      JsonUtils.mapFormat[String, BigDecimal]("type", "amount")

    val mandatoryMonetaryAmountMap: Option[Map[String, BigDecimal]] =
      (js \ "mandatoryMonetaryAmount").asOpt[Map[String, BigDecimal]]

    val optionalMonetaryAmountMap: Option[Map[String, BigDecimal]] =
      (js \ "optionalMonetaryAmount").asOpt[Map[String, BigDecimal]]

    val taxablePay: BigDecimal          = mandatoryMonetaryAmountMap.getOrElse(Map.empty).getOrElse("TaxablePayYTD", 0.0)
    val totalTax: BigDecimal            = mandatoryMonetaryAmountMap.getOrElse(Map.empty).getOrElse("TotalTaxYTD", 0.0)
    // TODO: Revert this back once a fix as been issued on API 1001
    // val studentLoan: Option[BigDecimal] = optionalMonetaryAmountMap.getOrElse(Map.empty).get("StudentLoansYTD")
    val studentLoan: Option[BigDecimal] = {
      val normalizedMap = optionalMonetaryAmountMap
        .getOrElse(Map.empty)
        .map { case (key, value) =>
          key.toUpperCase.replace("_", "") -> value
        }
      normalizedMap.get("STUDENTLOANSYTD")
    }

    JsSuccess(
      RtiPayment(
        paidOnDate = (js \ "pmtDate").as[LocalDate](using JsonUtils.rtiDateFormat),
        taxablePayYTD = taxablePay,
        totalTaxYTD = totalTax,
        studentLoansYTD = studentLoan
      )
    )
  }

  given writer: Writes[RtiPayment] = Json.writes[RtiPayment]

}

object RtiEmployment {

  given reader: Reads[RtiEmployment]  = (js: JsValue) =>
    for {
      sequenceNo   <- (js \ "sequenceNumber").validate[Int]
      officeNumber <- (js \ "empRefs" \ "officeNo").validate[String]
      payeRef      <- (js \ "empRefs" \ "payeRef").validate[String]
      currentPayId <- (js \ "currentPayId").validateOpt[String]
      payments     <- (js \ "payments" \ "inYear").validateOpt[List[RtiPayment]]
    } yield RtiEmployment(
      sequenceNo = sequenceNo,
      payeRef = payeRef,
      officeNumber = officeNumber,
      currentPayId = currentPayId,
      payments = payments.getOrElse(List.empty)
    )
  given writer: Writes[RtiEmployment] = Json.writes[RtiEmployment]

}

object RtiData {
  given reader: Reads[RtiData]  = (js: JsValue) =>
    for {
      nino        <- (js \ "request" \ "nino").validate[String]
      employments <- (js \ "individual" \ "employments" \ "employment").validate[List[RtiEmployment]]
    } yield RtiData(nino = nino, employments = employments)
  given writer: Writes[RtiData] = Json.writes[RtiData]
}
