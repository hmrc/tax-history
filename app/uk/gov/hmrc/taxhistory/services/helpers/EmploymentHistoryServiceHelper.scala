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

import play.Logger
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.api.{Allowance, Employment, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps._
import uk.gov.hmrc.taxhistory.model.taxhistory.{CompanyBenefit, EarlierYearUpdate}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait EmploymentHistoryServiceHelper extends TaxHistoryHelper with Auditable {

  def combineResult(iabdResponse:Either[HttpResponse,List[Iabd]],
                    rtiResponse:Either[HttpResponse,RtiData])
                   (npsEmployments: List[NpsEmployment])
                   (implicit headerCarrier: HeaderCarrier):HttpResponse={

    val iabdsOption = fetchResult(iabdResponse)
    val rtiOption = fetchResult(rtiResponse)

    val employments = npsEmployments.map {
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

        buildEmployment(npsEmployment = npsEmployment)
      }
    }

    val allowances= iabdsOption match {
      case None => Nil
      case Some(x) => new IabdsHelper(x).getAllowances()
    }

    val payAsYouEarn = PayAsYouEarn(employments=employments,allowances=allowances)
    httpOkPayAsYouEarnJsonPayload(payAsYouEarn)
  }

  def httpOkPayAsYouEarnJsonPayload(payload: PayAsYouEarn): HttpResponse = {
    HttpResponse(Status.OK,Some(Json.toJson(payload)))
  }



  def buildEmployment(npsEmployment: NpsEmployment): Employment = {
    Employment(
      employerName = npsEmployment.employerName,
      payeReference = getPayeRef(npsEmployment),
      startDate = npsEmployment.startDate,
      endDate = npsEmployment.endDate)
  }

  @Deprecated
  def buildEmployment(rtiEmploymentsOption: Option[List[RtiEmployment]], iabdsOption: Option[List[Iabd]], npsEmployment: NpsEmployment): Employment = {

    (rtiEmploymentsOption, iabdsOption) match {

      case (None | Some(Nil), None | Some(Nil)) => Employment(
        employerName = npsEmployment.employerName,
        payeReference = getPayeRef(npsEmployment),
        startDate = npsEmployment.startDate,
        endDate = npsEmployment.endDate)
      case (Some(Nil) | None, Some(y)) => {
        Employment(
          employerName = npsEmployment.employerName,
          payeReference = getPayeRef(npsEmployment),
       //   companyBenefits = getCompanyBenefits(y),
          startDate = npsEmployment.startDate,
          endDate = npsEmployment.endDate)
      }
      case (Some(x), Some(Nil) | None) => {
        val rtiPaymentInfo = getRtiPayment(x)
        Employment(
          employerName = npsEmployment.employerName,
          payeReference = getPayeRef(npsEmployment),
        //  taxablePayTotal = rtiPaymentInfo._1,
         // taxTotal = rtiPaymentInfo._2,
        //  earlierYearUpdates = convertRtiEYUToEYU(x),
          startDate = npsEmployment.startDate,
          endDate = npsEmployment.endDate
        )
      }
      case (Some(x), Some(y)) => {
        val rtiPaymentInfo = getRtiPayment(x)
        Employment(
          employerName = npsEmployment.employerName,
          payeReference = getPayeRef(npsEmployment),
        //  taxablePayTotal = rtiPaymentInfo._1,
        //  taxTotal = rtiPaymentInfo._2,
        //  earlierYearUpdates = convertRtiEYUToEYU(x),
        //  companyBenefits = getCompanyBenefits(y),
          startDate = npsEmployment.startDate,
          endDate = npsEmployment.endDate)
      }
      case _ => Employment(
        employerName = npsEmployment.employerName,
        payeReference = getPayeRef(npsEmployment),
        startDate = npsEmployment.startDate,
        endDate = npsEmployment.endDate)
    }

  }



  def getRtiPayment(rtiEmployments: List[RtiEmployment])={
    rtiEmployments.head.payments match {
      case Nil => (None,None)
      case matchingPayments => {
        val payment = matchingPayments.sorted.last
        (Some(payment.taxablePayYTD), Some(payment.totalTaxYTD))
      }
    }
  }

  private def getPayeRef(npsEmployment: NpsEmployment) = {
    npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber
  }







}


