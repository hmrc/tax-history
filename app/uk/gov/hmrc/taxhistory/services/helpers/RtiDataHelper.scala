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

import uk.gov.hmrc.tai.model.rti.RtiEmployment
import uk.gov.hmrc.taxhistory.model.api.PayAndTax
import uk.gov.hmrc.taxhistory.model.audit.DataEventDetail
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger


object RtiDataHelper extends TaxHistoryHelper with TaxHistoryLogger {

  def isMatch(npsEmployment: NpsEmployment, rtiEmployment: RtiEmployment): Boolean =
    (formatString(rtiEmployment.officeNumber) == formatString(npsEmployment.taxDistrictNumber)) &&
      (rtiEmployment.payeRef == npsEmployment.payeNumber)

  def isSubMatch(npsEmployment: NpsEmployment, rtiEmployment: RtiEmployment) = {
    (for {
      currentPayId <- rtiEmployment.currentPayId
      worksNumber  <- npsEmployment.worksNumber
    } yield {
      currentPayId == worksNumber
    }).getOrElse(false)
  }

  def matchEmployments(npsEmployments: List[NpsEmployment], rtiEmployments: List[RtiEmployment]): Map[NpsEmployment, List[RtiEmployment]] = {
    npsEmployments.map { npsEmployment =>
      val matchingRtiEmployments = rtiEmployments.filter(isMatch(npsEmployment, _))
      (npsEmployment, matchingRtiEmployments)
    }.toMap
  }

  /**
    * This function ensures that we have only one RTI employment matching each NPS employment,
    * and filters out any NPS employment for which there is no RTI counterpart.
    * It will send audit events as a side-effect if it finds and ambiguity which cannot be resolved.
    */
  def normalisedEmploymentMatches(npsEmployments: List[NpsEmployment], rtiEmployments: List[RtiEmployment]): Map[NpsEmployment, RtiEmployment] = {
    matchEmployments(npsEmployments, rtiEmployments).collect {
      case (nps, Nil)                   => nps -> None
      case (nps, uniqueRti :: Nil)      => nps -> Some(uniqueRti)
      case (nps, rti) if rti.length > 1 =>
        logger.warn(s"Multiple matching NPS employments found.")
        val subMatches = rti.filter(RtiDataHelper.isSubMatch(nps, _))
        subMatches match {
          case unique :: Nil => nps -> Some(unique)
          case _             => nps -> None
        }
    }.collect {
      case (nps, Some(rti)) => nps -> rti
    }
  }

  /**
    * Returns only those matches between NPS employments and RTI employments where there was ambiguity (non-unique match)
    * and the ambiguity could not be resolved.
    */
  def ambiguousEmploymentMatches(npsEmployments: List[NpsEmployment], rtiEmployments: List[RtiEmployment]): Map[NpsEmployment, List[RtiEmployment]] = {
    val rawMatches = matchEmployments(npsEmployments, rtiEmployments)
    val normalisedMatches = normalisedEmploymentMatches(npsEmployments, rtiEmployments)
    rawMatches.filter { case (k, v) =>
      (v.length > 1) && !normalisedMatches.keys.toList.contains(k) // if the normalised map omits the given key, it means it could not resolve the ambiguity.
    }
  }

  /**
    * Returns only those RTI employments which couldn't be matched to any NPS employments.
    */
  def employmentsOnlyInRTI(npsEmployments: List[NpsEmployment], rtiEmployments: List[RtiEmployment]): List[RtiEmployment] = {
    rtiEmployments.filter(rti => !npsEmployments.exists(RtiDataHelper.isMatch(_, rti)))
  }

  def buildEmploymentDataEventDetails(nino: String, rtiEmployments: List[RtiEmployment]): Seq[DataEventDetail] =
    rtiEmployments.map(rE =>
      DataEventDetail(
        Map("nino" -> nino, "payeRef" -> rE.payeRef, "officeNumber" -> rE.officeNumber, "currentPayId" -> rE.currentPayId.getOrElse("")))
    )

  def convertToPayAndTax(rtiEmployments: List[RtiEmployment]): PayAndTax = {
    val employment = rtiEmployments.head
    val eyus = employment.earlierYearUpdates.map(_.toEarlierYearUpdate)
    val nonEmptyEyus = eyus.filter(eyu => eyu.taxablePayEYU != 0 && eyu.taxEYU != 0)

    employment.payments match {
      case Nil => PayAndTax(earlierYearUpdates = nonEmptyEyus)
      case matchingPayments => {
        val payment = matchingPayments.sorted.last
        PayAndTax(taxablePayTotal = Some(payment.taxablePayYTD),
          taxTotal = Some(payment.totalTaxYTD),
          paymentDate=Some(payment.paidOnDate),
          earlierYearUpdates = nonEmptyEyus)
      }
    }
  }
}
