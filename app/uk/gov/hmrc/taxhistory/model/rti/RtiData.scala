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

package uk.gov.hmrc.tai.model.rti

import com.github.nscala_time.time.Imports._
import org.joda.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.taxhistory.model.api.EarlierYearUpdate
import uk.gov.hmrc.taxhistory.model.utils.JsonUtils

case class RtiData(nino:String,
                   employments:List[RtiEmployment])

case class RtiEmployment(sequenceNo:Int,
                         officeNumber:String,
                         payeRef:String,
                         currentPayId: Option[String]= None,
                         payments:List[RtiPayment],
                         earlierYearUpdates:List[RtiEarlierYearUpdate])

case class RtiPayment(
                      paidOnDate:LocalDate,
                      taxablePayYTD:BigDecimal,
                      totalTaxYTD:BigDecimal) extends Ordered[RtiPayment] {
  def compare(that: RtiPayment) = this.paidOnDate compare that.paidOnDate
}

case class RtiEarlierYearUpdate(taxablePayDelta:BigDecimal,
                                totalTaxDelta:BigDecimal,
                                receivedDate:LocalDate) {

  def toEarlierYearUpdate: EarlierYearUpdate = {
    EarlierYearUpdate(
      taxablePayEYU = taxablePayDelta,
      taxEYU = totalTaxDelta,
      receivedDate = receivedDate
    )
  }
}


object RtiPayment {
  implicit val reader = new Reads[RtiPayment]{
     def reads(js: JsValue): JsResult[RtiPayment] = {
       implicit val stringMapFormat = JsonUtils.mapFormat[String,BigDecimal]("type", "amount")
       val mandatoryMonetaryAmountMap = (js \ "mandatoryMonetaryAmount").as[Map[String, BigDecimal]]

       JsSuccess(
         RtiPayment(
           paidOnDate = (js \ "pmtDate").as[LocalDate](JsonUtils.rtiDateFormat),
           taxablePayYTD = mandatoryMonetaryAmountMap("TaxablePayYTD"),
           totalTaxYTD = mandatoryMonetaryAmountMap("TotalTaxYTD")))
     }
   }

  implicit val writer = Json.writes[RtiPayment]
}

object RtiEarlierYearUpdate {
  implicit val reader = new Reads[RtiEarlierYearUpdate]{
    def reads(js: JsValue): JsResult[RtiEarlierYearUpdate] = {
      implicit val stringMapFormat = JsonUtils.mapFormat[String,BigDecimal]("type", "amount")
      val mandatoryMonetaryAmountMap = (js \ "optionalAdjustmentAmount").as[Map[String, BigDecimal]]
      val receivedDate = (js \ "rcvdDate").as[LocalDate](JsonUtils.rtiDateFormat)

      JsSuccess(
        RtiEarlierYearUpdate(taxablePayDelta = mandatoryMonetaryAmountMap("TaxablePayDelta"),
          totalTaxDelta = mandatoryMonetaryAmountMap("TotalTaxDelta"),
          receivedDate = receivedDate)
      )
    }
  }
  implicit val writer = Json.writes[RtiEarlierYearUpdate]
}

object RtiEmployment {
  implicit val reader = new Reads[RtiEmployment] {
    def reads(js: JsValue): JsResult[RtiEmployment] = {
        for {
          sequenceNo <- (js \ "sequenceNumber" ).validate[Int]
          officeNumber <- (js \ "empRefs" \ "officeNo").validate[String]
          payeRef <- (js \ "empRefs" \ "payeRef").validate[String]
          currentPayId <- (js \ "currentPayId").validateOpt[String]
          payments <- (js \ "payments" \ "inYear").validate[List[RtiPayment]]
          earlierYearUpdates <- JsSuccess((js \ "payments" \ "eyu").asOpt[List[RtiEarlierYearUpdate]])
        } yield {
            RtiEmployment(
              sequenceNo = sequenceNo,
              payeRef = payeRef,
              officeNumber = officeNumber,
              currentPayId = currentPayId,
              payments = payments,
              earlierYearUpdates = earlierYearUpdates.getOrElse(Nil)
            )
        }
    }
  }
  implicit val writer = Json.writes[RtiEmployment]
}

object RtiData {
  implicit val reader = new Reads[RtiData] {
    def reads(js: JsValue): JsResult[RtiData] = {
      for {
        nino <- (js \ "request" \ "nino").validate[String]
        employments <- (js \ "individual" \ "employments" \ "employment").validate[List[RtiEmployment]]
      } yield {
          RtiData( nino = nino, employments = employments )
      }
    }
  }
  implicit val writer = Json.writes[RtiData]
}

