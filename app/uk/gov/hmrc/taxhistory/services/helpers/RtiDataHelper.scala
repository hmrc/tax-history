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

import play.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.model.api.{EarlierYearUpdate, PayAndTax}
import uk.gov.hmrc.taxhistory.model.nps.{NpsEmployment, NpsTaxAccount}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RtiDataHelper(val rtiData: RtiData) extends TaxHistoryHelper {

  val rtiEmployments = this.rtiData.employments

  def onlyInRTI(npsEmployments: List[NpsEmployment]):List[RtiEmployment]={
    rtiEmployments.filter(rti => !npsEmployments.exists(nps => isMatch(nps,rti)))
  }

  def isMatch(npsEmployment :NpsEmployment, rtiEmployment :RtiEmployment):Boolean={
    (formatString(rtiEmployment.officeNumber) == formatString(npsEmployment.taxDistrictNumber)) &&
      (rtiEmployment.payeRef == npsEmployment.payeNumber)
  }

  def getMatchedRtiEmployments(npsEmployments: NpsEmployment)
                              (auditEvent: List[RtiEmployment]=> Future[List[Unit]])(implicit headerCarrier: HeaderCarrier): List[RtiEmployment] = {

    fetchFilteredList(rtiEmployments){
      (rtiEmployment) =>
        isMatch(npsEmployments, rtiEmployment)
    } match {
      case (matchingEmp :: Nil) =>List(matchingEmp)
      case start :: end => {
        Logger.warn("Multiple matching rti employments found.")
        val subMatches = (start :: end).filter {
          rtiEmployment => {
            rtiEmployment.currentPayId.isDefined &&
              npsEmployments.worksNumber.isDefined &&
              rtiEmployment.currentPayId == npsEmployments.worksNumber
          }
        }
        subMatches match {
          case first :: Nil => List(first)
          case x  => {
            auditEvent(x)
            Nil
          }
        }
      }
      case _ => Nil //Auditing will happen in the function onlyInRTI for this case
    }
  }


  def auditEvent(x: List[RtiEmployment])
                        (eventType:String)(sendAuditEvent  : (String,Map[String,String])=> Unit)
                        (implicit headerCarrier: HeaderCarrier) = {
    Future {
      for {
        r <- x
      } yield {
        val x = Map(
          "nino" -> rtiData.nino,
          "payeRef" -> r.payeRef,
          "officeNumber" -> r.officeNumber,
          "currentPayId" -> r.currentPayId.fold("")(a => a))
          sendAuditEvent(eventType,x)
      }
    }
  }

}

object RtiDataHelper {

  def convertToPayAndTax(rtiEmployments: List[RtiEmployment], npsTaxAccount: Option[NpsTaxAccount]): PayAndTax ={
    val employment = rtiEmployments.head
    val eyuList = convertRtiEYUToEYU(employment)
    (employment.payments, npsTaxAccount) match {
      case (Nil,None) => PayAndTax(earlierYearUpdates = eyuList)
      case (Nil,Some(taxAccount)) => {
        PayAndTax(earlierYearUpdates = eyuList,
          outstandingDebtRestriction = taxAccount.getOutStandingDebt(),
          underpaymentAmount = taxAccount.getUnderPayment(),
          actualPUPCodedInCYPlusOneTaxYear = taxAccount.getActualPupCodedInCYPlusOne())
      }
      case (matchingPayments,None) => {
        val payment =matchingPayments.sorted.last
        PayAndTax(taxablePayTotal = Some(payment.taxablePayYTD),
          taxTotal = Some(payment.totalTaxYTD),
          paymentDate=Some(payment.paidOnDate),
          earlierYearUpdates = eyuList)
      }
      case (matchingPayments,Some(taxAccount)) => {
        val payment =matchingPayments.sorted.last
        PayAndTax(taxablePayTotal = Some(payment.taxablePayYTD),
          taxTotal = Some(payment.totalTaxYTD),
          paymentDate=Some(payment.paidOnDate),
          earlierYearUpdates = eyuList,
          outstandingDebtRestriction = taxAccount.getOutStandingDebt(),
          underpaymentAmount = taxAccount.getUnderPayment(),
          actualPUPCodedInCYPlusOneTaxYear = taxAccount.getActualPupCodedInCYPlusOne())
      }
    }
  }

  def convertRtiEYUToEYU(rtiEmployment: RtiEmployment): List[EarlierYearUpdate] = {
    rtiEmployment.earlierYearUpdates.map(eyu => {
      EarlierYearUpdate(
        taxablePayEYU =  eyu.taxablePayDelta,
        taxEYU = eyu.totalTaxDelta,
        receivedDate = eyu.receivedDate)
    }).filter(x =>x.taxablePayEYU != 0 && x.taxEYU != 0)
  }

  def getPayeRef(npsEmployment: NpsEmployment) = {
    npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber
  }
}
