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

package uk.gov.hmrc.tai.model.rti

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.data.validation.ValidationError
import play.api.libs.json._
import uk.gov.hmrc.taxhistory.model.utils.JsonUtils

case class RtiData(nino:String,
                   employments:List[RtiEmployment])

case class RtiEmployment(sequenceNo:Int,
                         officeNumber:String,
                         payeRef:String,
                         currentPayId: Option[String]= None,
                         payments:List[RtiPayment],
                         endOfYearUpdates:List[RtiEndOfYearUpdate])

case class RtiPayment(taxablePay:BigDecimal,
                      taxablePayYTD:BigDecimal,
                      taxDeductedOrRefunded:BigDecimal,
                      totalTaxYTD:BigDecimal)

case class RtiEndOfYearUpdate(taxablePayDelta:BigDecimal,
                              totalTaxDelta:BigDecimal,
                              receivedDate:LocalDate)


object RtiPayment {
   val reader = new Reads[RtiPayment]{
     def reads(js: JsValue): JsResult[RtiPayment] = {
       implicit val stringMapFormat = JsonUtils.mapFormat[String,BigDecimal]("type", "amount")
       val mandatoryMonetaryAmountMap = (js \ "mandatoryMonetaryAmount").as[Map[String, BigDecimal]]

       JsSuccess(
         RtiPayment(taxablePay = mandatoryMonetaryAmountMap("TaxablePay"),
           taxablePayYTD = mandatoryMonetaryAmountMap("TaxablePay"),
           taxDeductedOrRefunded = mandatoryMonetaryAmountMap("TaxDeductedOrRefunded"),
           totalTaxYTD = mandatoryMonetaryAmountMap("TotalTaxYTD")))
     }
   }
  implicit val formats = Json.format[RtiPayment]
}

object RtiEndOfYearUpdate {
  val reader = new Reads[RtiEndOfYearUpdate]{
    def reads(js: JsValue): JsResult[RtiEndOfYearUpdate] = {
      implicit val stringMapFormat = JsonUtils.mapFormat[String,BigDecimal]("type", "amount")
      val mandatoryMonetaryAmountMap = (js \ "optionalAdjustmentAmount").as[Map[String, BigDecimal]]
      val receivedDate = (js \ "rcvdDate").as[LocalDate]
      JsSuccess(
        RtiEndOfYearUpdate(taxablePayDelta = mandatoryMonetaryAmountMap("TaxablePayDelta"),
          totalTaxDelta = mandatoryMonetaryAmountMap("TotalTaxDelta"),
          receivedDate = receivedDate
         ))
    }


    implicit val formatLocalDate: Format[LocalDate] = Format(
      new Reads[LocalDate]{
        val dateRegex = """^(\d\d\d\d)-(\d\d)-(\d\d)$""".r
        override def reads(json: JsValue): JsResult[LocalDate] = json match {
          case JsString(dateRegex(y, m, d)) =>
            JsSuccess(new LocalDate(y.toInt, m.toInt, d.toInt))
          case invalid => JsError(ValidationError(
            s"Invalid date format [yyyy-MM-dd]: $invalid"))
        }
      },
      new Writes[LocalDate]{
        val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
        override def writes(date: LocalDate): JsValue =
          JsString(dateFormat.print(date))
      }
    )
  }
  implicit val formats = Json.format[RtiEndOfYearUpdate]
}

object RtiEmployment {
  val reader = new Reads[RtiEmployment] {
    def reads(js: JsValue): JsResult[RtiEmployment] = {
        for {
          sequenceNo <- (js \ "sequenceNumber" ).validate[Int]
          officeNumber <- (js \ "empRefs" \ "officeNo").validate[String]
          payeRef <- (js \ "empRefs" \ "payeRef").validate[String]
          currentPayId <- (js \ "currentPayId").validate[String]
          payments <- (js \ "payments" \ "inYear").validate[List[RtiPayment]](Reads.list(RtiPayment.reader))
          endOfYearUpdates <- JsSuccess((js \ "payments" \ "eyu").asOpt[List[RtiEndOfYearUpdate]](Reads.list(RtiEndOfYearUpdate.reader)))
        } yield {
            RtiEmployment(
              sequenceNo = sequenceNo,
              payeRef = payeRef,
              officeNumber = officeNumber,
              currentPayId = Some(currentPayId),
              payments = payments,
              endOfYearUpdates = endOfYearUpdates.getOrElse(Nil)
            )
        }
    }
  }
  implicit val formats = Json.format[RtiEmployment]
}

object RtiData {
  val reader = new Reads[RtiData] {
    def reads(js: JsValue): JsResult[RtiData] = {

      for {
        nino <- (js \ "request" \ "nino").validate[String]
        employments <- (js \ "individual" \ "employments" \ "employment").validate[List[RtiEmployment]](Reads.list(RtiEmployment.reader))
      } yield {

          RtiData(
            nino = nino,
            employments = employments
          )

      }
    }
  }
  implicit val formats = Json.format[RtiData]
}

