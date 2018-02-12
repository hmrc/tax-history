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
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.utils.Logging


object EmploymentMatchingHelper extends TaxHistoryHelper with Logging {

  def isMatch(npsEmployment: NpsEmployment, rtiEmployment: RtiEmployment): Boolean =
    (formatString(rtiEmployment.officeNumber) == formatString(npsEmployment.taxDistrictNumber)) &&
      (rtiEmployment.payeRef == npsEmployment.payeNumber)

  def isSubMatch(npsEmployment: NpsEmployment, rtiEmployment: RtiEmployment): Boolean = {
    (for {
      currentPayId <- rtiEmployment.currentPayId
      worksNumber  <- npsEmployment.worksNumber
    } yield {
      currentPayId == worksNumber
    }).getOrElse(false)
  }

  /**
    * This function returns all matches between NPS employments and RTI employments, including ambiguous ones and empty ones.
    * It can be imagined as a simple 'left join' between NPS employments and RTI employments.
    */
  private def rawMatchedEmployments(npsEmployments: List[NpsEmployment], rtiEmployments: List[RtiEmployment]): Map[NpsEmployment, List[RtiEmployment]] = {
    npsEmployments.map { npsEmployment =>
      val matchingRtiEmployments = rtiEmployments.filter(isMatch(npsEmployment, _))
      (npsEmployment, matchingRtiEmployments)
    }.toMap
  }

  /**
    * This function matches NpsEmployments to RtiEmployments, ensuring that we have only one RTI employment
    * matching each NPS employment, and filters out any NPS employment for which there is no RTI counterpart
    * of for which there is a non-resolvable ambiguity between two or more RTI employments.
    */
  def matchedEmployments(npsEmployments: List[NpsEmployment], rtiEmployments: List[RtiEmployment]): Map[NpsEmployment, RtiEmployment] = {
    rawMatchedEmployments(npsEmployments, rtiEmployments).collect {
      case (nps, Nil)                   => nps -> None            // - No RTI employments found for this NPS employment
      case (nps, uniqueRti :: Nil)      => nps -> Some(uniqueRti) // - A single RTI employments found for this NPS employment (happy path)
      case (nps, rti) if rti.length > 1 =>                        // - More than a RTI employment may be a match. We'll try to resolve the ambiguity.
        val subMatches = rti.filter(EmploymentMatchingHelper.isSubMatch(nps, _))
        subMatches match {
          case unique :: Nil => nps -> Some(unique) // Ambiguity resolved
          case _             => nps -> None         // Not resolved
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
    val rawMatches = rawMatchedEmployments(npsEmployments, rtiEmployments)
    val normalisedMatches = matchedEmployments(npsEmployments, rtiEmployments)
    rawMatches.filter { case (k, v) =>
      (v.length > 1) && !normalisedMatches.keys.toList.contains(k) // if the normalised map omits the given key, it means it could not resolve the ambiguity.
    }
  }

  /**
    * Returns only those RTI employments which couldn't be matched to any NPS employments.
    */
  def unmatchedRtiEmployments(npsEmployments: List[NpsEmployment], rtiEmployments: List[RtiEmployment]): List[RtiEmployment] = {
    val allEmployments: List[RtiEmployment]     = rtiEmployments
    val matchedEmployments: List[RtiEmployment] = rawMatchedEmployments(npsEmployments, rtiEmployments).values.toList.flatten
    (allEmployments.toSet -- matchedEmployments.toSet).toList
  }

}
