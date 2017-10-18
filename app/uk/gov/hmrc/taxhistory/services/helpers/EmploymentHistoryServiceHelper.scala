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

package uk.gov.hmrc.taxhistory.services.helpers

import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.api.{Employment, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps._

trait EmploymentHistoryServiceHelper extends TaxHistoryHelper with Auditable {

  def combineResult(iabdResponse:Either[HttpResponse,List[Iabd]],
                    rtiResponse:Either[HttpResponse,RtiData])
                   (npsEmployments: List[NpsEmployment])
                   (implicit headerCarrier: HeaderCarrier):HttpResponse={

    val iabdsOption = fetchResult(iabdResponse)
    val rtiOption = fetchResult(rtiResponse)

    val payAsYouEarnList = npsEmployments.map {
      npsEmployment => {
        val companyBenefits = iabdsOption.map {
          iabds => {
            new IabdsHelper(iabds).getMatchedCompanyBenefits(npsEmployment)
          }
        }
        val rtiEmployments = rtiOption.map {
          rtiData => {
           val rtiDataHelper =  new RtiDataHelper(rtiData)

            rtiDataHelper.auditEvent(rtiDataHelper.onlyInRTI(npsEmployments))("only-in-rti"){
              (x, y) =>   sendDataEvent(transactionName = "Paye for Agents",detail = y,eventType = x)
            }

            rtiDataHelper.getMatchedRtiEmployments(npsEmployment){
              rtiEmployments => {
                rtiDataHelper.auditEvent(rtiEmployments)("miss-match"){
                  (x, y) =>   sendDataEvent(transactionName = "Paye for Agents",detail = y,eventType = x)
                }
              }
            }

          }
        }

        buildPayAsYouEarnList(rtiEmploymentsOption=rtiEmployments,iabdsOption=companyBenefits,npsEmployment = npsEmployment)
      }
    }

    val allowances= iabdsOption match {
      case None => Nil
      case Some(x) => new IabdsHelper(x).getAllowances()
    }

    val payAsYouEarn =  payAsYouEarnList.fold(PayAsYouEarn(allowances=allowances))((paye1, paye2) => {
      PayAsYouEarn(employments = paye1.employments ::: paye2.employments,
        allowances = paye1.allowances ::: paye2.allowances,
        benefits = Some(paye1.benefits.getOrElse(Map()) ++ paye2.benefits.getOrElse(Map())),
        payAndTax = Some(paye1.payAndTax.getOrElse(Map()) ++ paye2.payAndTax.getOrElse(Map())))
    })

    httpOkPayAsYouEarnJsonPayload(payAsYouEarn)
  }

  def httpOkPayAsYouEarnJsonPayload(payload: PayAsYouEarn): HttpResponse = {
    HttpResponse(Status.OK,Some(Json.toJson(payload)))
  }

  def buildPayAsYouEarnList(rtiEmploymentsOption: Option[List[RtiEmployment]], iabdsOption: Option[List[Iabd]], npsEmployment: NpsEmployment): PayAsYouEarn = {

    val emp = convertToEmployment(npsEmployment)
    (rtiEmploymentsOption, iabdsOption) match {

      case (None | Some(Nil), None | Some(Nil)) => {
        PayAsYouEarn(employments = List(emp))
      }
      case (Some(Nil) | None, Some(y)) => {
        lazy val benefits = Map(emp.employmentId.toString -> new IabdsHelper(y).getCompanyBenefits())
        PayAsYouEarn(employments=List(emp),benefits=Some(benefits))
      }
      case (Some(x), Some(Nil) | None) => {
        lazy val payAndTaxMap = Map(emp.employmentId.toString -> RtiDataHelper.convertToPayAndTax(x))
        PayAsYouEarn(employments = List(emp),payAndTax=Some(payAndTaxMap))
      }
      case (Some(x), Some(y)) => {
        lazy val payAndTaxMap = Map(emp.employmentId.toString -> RtiDataHelper.convertToPayAndTax(x))
        lazy val benefits = Map(emp.employmentId.toString -> new IabdsHelper(y).getCompanyBenefits())
        PayAsYouEarn(employments=List(emp),benefits=Some(benefits),payAndTax = Some(payAndTaxMap))
      }
      case _ => {
        PayAsYouEarn(employments = List(emp))
      }
    }

  }

 def convertToEmployment(npsEmployment: NpsEmployment):Employment = {
   Employment(
     employerName = npsEmployment.employerName,
     payeReference = RtiDataHelper.getPayeRef(npsEmployment),
     startDate = npsEmployment.startDate,
     endDate = npsEmployment.endDate)
 }

  def getRtiPayment(rtiEmployments: List[RtiEmployment]):(Option[BigDecimal],Option[BigDecimal])={
    rtiEmployments.head.payments match {
      case Nil => (None,None)
      case matchingPayments =>
        val payment = matchingPayments.sorted.max
        (Some(payment.taxablePayYTD), Some(payment.totalTaxYTD))
    }
  }
}


