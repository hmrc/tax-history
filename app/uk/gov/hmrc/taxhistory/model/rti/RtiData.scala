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

import play.api.libs.json._
import uk.gov.hmrc.taxhistory.model.utils.JsonUtils

case class RtiData(nino:String,
                   taxYear:String,
                   employments:List[Employment])

case class Employment(sequenceNo:Int,
                      payeRef:String,
                      currentPayId: Option[String]= None,
                      payments:List[Payment])

case class Payment(taxablePay:BigDecimal,
                   taxablePayYTD:BigDecimal,
                   taxDeductedOrRefunded:BigDecimal,
                   totalTaxYTD:BigDecimal)

object Payment {
   val reader = new Reads[Payment]{
     def reads(js: JsValue): JsResult[Payment] = {
       implicit val stringMapFormat = JsonUtils.mapFormat[String,BigDecimal]("type", "amount")

       val mandatoryMonetaryAmountMap = (js \ "mandatoryMonetaryAmount").as[Map[String, BigDecimal]]

       JsSuccess(
         Payment(taxablePay = mandatoryMonetaryAmountMap("TaxablePay"),
           taxablePayYTD = mandatoryMonetaryAmountMap("TaxablePay"),
           taxDeductedOrRefunded = mandatoryMonetaryAmountMap("TaxDeductedOrRefunded"),
           totalTaxYTD = mandatoryMonetaryAmountMap("TotalTaxYTD")))
     }
   }
  implicit val formats = Json.format[Payment]
}

object Employment {
  val reader = new Reads[Employment] {
    def reads(js: JsValue): JsResult[Employment] = {
        for {
          payeRef <- (js \ "empRefs" \ "payeRef").validate[String]
          currentPayId <- (js \ "currentPayId").validate[String]
          payments <- (js \ "payments" \ "inYear").validate[List[Payment]](Reads.list(Payment.reader))
        } yield {

            Employment(sequenceNo = 0,
              payeRef = payeRef,
              currentPayId = Some(currentPayId),
              payments = payments
            )

        }
    }
  }
  implicit val formats = Json.format[Employment]
}

object RtiData {
  val reader = new Reads[RtiData] {
    def reads(js: JsValue): JsResult[RtiData] = {
      for {
        nino <- (js \ "request" \ "nino").validate[String]
        taxYear <- (js \ "request" \ "relatedTaxYear").validate[String]
        employments <- (js \ "individual" \ "employments" \ "employment").validate[List[Employment]](Reads.list(Employment.reader))
      } yield {

          RtiData(
            nino = nino,
            taxYear = taxYear,
            employments = employments
          )

      }
    }
  }
  implicit val formats = Json.format[RtiData]
}

