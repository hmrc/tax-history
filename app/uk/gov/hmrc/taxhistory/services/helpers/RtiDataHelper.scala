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

import com.google.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.rti.RtiEmployment
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.api.{EarlierYearUpdate, PayAndTax}
import uk.gov.hmrc.taxhistory.model.audit.{DataEventDetail, NpsRtiMismatch, OnlyInRti, PAYEForAgents}
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger


class RtiDataHelper @Inject()(val auditable: Auditable) extends TaxHistoryHelper with TaxHistoryLogger{

  def auditOnlyInRTI(nino: String, npsEmployments: List[NpsEmployment], rtiEmployments: List[RtiEmployment])
                    (implicit headerCarrier: HeaderCarrier): Unit = {
    val employments = rtiEmployments.filter(rti => !npsEmployments.exists(nps => isMatch(nps, rti)))
    auditable.sendDataEvents(
      transactionName = PAYEForAgents,
      details = buildEmploymentDataEventDetails(nino, employments),
      eventType = OnlyInRti)
  }

  private def isMatch(npsEmployment: NpsEmployment, rtiEmployment: RtiEmployment): Boolean =
    (formatString(rtiEmployment.officeNumber) == formatString(npsEmployment.taxDistrictNumber)) &&
      (rtiEmployment.payeRef == npsEmployment.payeNumber)

  private def isSubMatch(npsEmployment: NpsEmployment, rtiEmployment: RtiEmployment) =
    rtiEmployment.currentPayId.isDefined &&
      npsEmployment.worksNumber.isDefined &&
      rtiEmployment.currentPayId == npsEmployment.worksNumber

  def getMatchedRtiEmployments(nino: String, npsEmployment: NpsEmployment, rtiEmployments: List[RtiEmployment])
                              (implicit headerCarrier: HeaderCarrier): List[RtiEmployment] = {

    rtiEmployments.filter(rtiEmployment => isMatch(npsEmployment, rtiEmployment)) match {
      case matchingEmp :: Nil => List(matchingEmp)
      case start :: end =>
        logger.warn("Multiple matching rti employments found.")
        (start :: end).filter(rtiEmployment => isSubMatch(npsEmployment, rtiEmployment)) match {
          case matchingEmp :: Nil => List(matchingEmp)
          case mismatchedEmployments =>
            auditable.sendDataEvents(transactionName = PAYEForAgents, details = buildEmploymentDataEventDetails(nino, mismatchedEmployments), eventType = NpsRtiMismatch)
            Nil
        }
      case Nil => Nil //Auditing will happen in the function onlyInRTI for this case
    }
  }

  def buildEmploymentDataEventDetails(nino: String, rtiEmployments: List[RtiEmployment]): Seq[DataEventDetail] =
    rtiEmployments.map(rE =>
      DataEventDetail(
        Map("nino" -> nino, "payeRef" -> rE.payeRef, "officeNumber" -> rE.officeNumber, "currentPayId" -> rE.currentPayId.getOrElse("")))
    )
}

object RtiDataHelper {

  def convertToPayAndTax(rtiEmployments: List[RtiEmployment]): PayAndTax = {
    val employment = rtiEmployments.head
    val eyuList = convertRtiEYUToEYU(employment)
    employment.payments match {
      case Nil => PayAndTax(earlierYearUpdates = eyuList)
      case matchingPayments => {
        val payment = matchingPayments.sorted.last
        PayAndTax(taxablePayTotal = Some(payment.taxablePayYTD),
          taxTotal = Some(payment.totalTaxYTD),
          paymentDate=Some(payment.paidOnDate),
          earlierYearUpdates = eyuList)
      }
    }
  }

  def convertRtiEYUToEYU(rtiEmployment: RtiEmployment): List[EarlierYearUpdate] = {
    rtiEmployment.earlierYearUpdates.map(eyu => {
      EarlierYearUpdate(
        taxablePayEYU = eyu.taxablePayDelta,
        taxEYU = eyu.totalTaxDelta,
        receivedDate = eyu.receivedDate)
    }).filter(x => x.taxablePayEYU != 0 && x.taxEYU != 0)
  }
}
