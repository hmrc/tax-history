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

package uk.gov.hmrc.taxhistory.services.helpers

import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.api.{Allowance, Employment, PayAsYouEarn, TaxAccount}
import uk.gov.hmrc.taxhistory.model.nps._

trait EmploymentHistoryServiceHelper extends TaxHistoryHelper with Auditable {

  def combineResult(iabdResponse: Either[HttpResponse,List[Iabd]],
                    rtiResponse: Either[HttpResponse,RtiData],
                    taxAccResponse: Either[HttpResponse,Option[NpsTaxAccount]])
                   (npsEmployments: List[NpsEmployment])
                   (implicit headerCarrier: HeaderCarrier): PayAsYouEarn = {

    val iabdsOption  = iabdResponse.right.toOption
    val rtiOption    = rtiResponse.right.toOption
    val taxAccOption = taxAccResponse.right.toOption

    val payAsYouEarnList = npsEmployments.map { npsEmployment =>
      val companyBenefits = iabdsOption.map { iabds =>
        new IabdsHelper(iabds).getMatchedCompanyBenefits(npsEmployment)
      }
      val rtiEmployments = rtiOption.map { rtiData =>
        val rtiDataHelper =  new RtiDataHelper(rtiData)
        rtiDataHelper.auditEvent(rtiDataHelper.onlyInRTI(npsEmployments))("only-in-rti") {
          (x, y) => sendDataEvent(transactionName = "Paye for Agents",detail = y, eventType = x)
        }
        rtiDataHelper.getMatchedRtiEmployments(npsEmployment) { rtiEmployments =>
          rtiDataHelper.auditEvent(rtiEmployments)("miss-match") {
            (x, y) => sendDataEvent(transactionName = "Paye for Agents", detail = y, eventType = x)
          }
        }
      }

      buildPayAsYouEarnList(rtiEmploymentsOption = rtiEmployments, iabdsOption = companyBenefits, npsEmployment = npsEmployment)
    }

    val allowances = iabdsOption match {
      case None => Nil
      case Some(x) => new IabdsHelper(x).getAllowances()
    }
    val taxAccount = taxAccOption.flatten.map(_.toTaxAccount)
    val payAsYouEarn =  mergeIntoSinglePayAsYouEarn(payAsYouEarnList = payAsYouEarnList, allowances=allowances, taxAccount=taxAccount)

    payAsYouEarn
  }

  def mergeIntoSinglePayAsYouEarn(payAsYouEarnList:List[PayAsYouEarn], allowances:List[Allowance], taxAccount: Option[TaxAccount]):PayAsYouEarn = {
    payAsYouEarnList.reduce((p1, p2) => {
      PayAsYouEarn(
        employments = p1.employments ::: p2.employments,
        benefits =  (p1.benefits ++ p2.benefits).reduceLeftOption((a,b)=>a++b),
        payAndTax = (p1.payAndTax ++ p2.payAndTax).reduceLeftOption((a,b)=>a++b))
    }).copy(allowances=allowances, taxAccount=taxAccount)
  }

  def buildPayAsYouEarnList(rtiEmploymentsOption: Option[List[RtiEmployment]], iabdsOption: Option[List[Iabd]],
                            npsEmployment: NpsEmployment): PayAsYouEarn = {

    val emp = npsEmployment.toEmployment
    (rtiEmploymentsOption, iabdsOption) match {
      case (None | Some(Nil), None | Some(Nil)) =>
        PayAsYouEarn(employments = List(emp))
      case (Some(Nil) | None, Some(y)) =>
        val benefits = Map(emp.employmentId.toString -> new IabdsHelper(y).getCompanyBenefits())
        PayAsYouEarn(employments=List(emp),benefits=Some(benefits))
      case (Some(x), Some(Nil) | None) =>
        val payAndTaxMap = Map(emp.employmentId.toString -> RtiDataHelper.convertToPayAndTax(x))
        PayAsYouEarn(employments = List(emp),payAndTax=Some(payAndTaxMap))
      case (Some(x), Some(y)) =>
        val payAndTaxMap = Map(emp.employmentId.toString -> RtiDataHelper.convertToPayAndTax(x))
        val benefits = Map(emp.employmentId.toString -> new IabdsHelper(y).getCompanyBenefits())
        PayAsYouEarn(employments=List(emp),benefits=Some(benefits),payAndTax = Some(payAndTaxMap))
      case _ => PayAsYouEarn(employments = List(emp))
    }
  }

}