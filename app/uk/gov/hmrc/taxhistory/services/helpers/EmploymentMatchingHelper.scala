/*
 * Copyright 2019 HM Revenue & Customs
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

  private def hasSameSequenceNo(nps: NpsEmployment, rti: RtiEmployment): Boolean =
    nps.sequenceNumber == rti.sequenceNo

  private def matchEmploymentsFromSameEmployerSamePayrollId(optPayrollId: Option[String],
                                                            npsWithSamePayrollId: List[NpsEmployment],
                                                            rtiWithSamePayrollId: List[RtiEmployment]): Map[NpsEmployment, RtiEmployment] = {
    val isPayrollIdPresent = optPayrollId.isDefined
    val hasOnlyOneEmployment = npsWithSamePayrollId.length == 1 && rtiWithSamePayrollId.length == 1

    if(!isPayrollIdPresent) logger.warn("worksNumber/currentPayId is missing - will rely on sequenceNumber to match.")

    (for {
      nps <- npsWithSamePayrollId
      rti <- rtiWithSamePayrollId
      if (isPayrollIdPresent && hasOnlyOneEmployment) || hasSameSequenceNo(nps, rti)
    } yield nps -> rti).toMap
  }

  private def matchEmploymentsFromSameEmployer(npsSameEmployer: List[NpsEmployment],
                                               rtiSameEmployer: List[RtiEmployment]): Map[NpsEmployment, RtiEmployment] = {
    if (npsSameEmployer.length == 1 && rtiSameEmployer.length == 1) {
      Map(npsSameEmployer.head -> rtiSameEmployer.head)
    }
    else {
      val npsByPayrollId: Map[Option[String], List[NpsEmployment]] = npsSameEmployer.groupBy(_.worksNumber)
      val rtiByPayrollId: Map[Option[String], List[RtiEmployment]] = rtiSameEmployer.groupBy(_.currentPayId)

      npsByPayrollId.flatMap { case (optPayrollId, npsWithSamePayrollId) =>
        val optRtiWithSamePayrollId = rtiByPayrollId.get(optPayrollId)

        optRtiWithSamePayrollId match {
          case Some(rtiSamePayrollId) => { // 1 or more RTI employments have the same payroll ID
            matchEmploymentsFromSameEmployerSamePayrollId(optPayrollId, npsWithSamePayrollId, rtiSamePayrollId)
          }
          case None => Map.empty[NpsEmployment, RtiEmployment] // No RTI employments have same payroll ID
        }
      }
    }
  }

  def matchEmployments(npsEmployments: List[NpsEmployment], rtiEmployments: List[RtiEmployment]): Map[NpsEmployment, RtiEmployment] = {
    val npsByEmployer: Map[(String, String), List[NpsEmployment]] = npsEmployments.groupBy(nps => (nps.taxDistrictNumber, nps.payeNumber))
    val rtiByEmployer: Map[(String, String), List[RtiEmployment]] = rtiEmployments.groupBy(rti => (rti.officeNumber, rti.payeRef))

    npsByEmployer.collect {
      case (employerKey, npsSameEmployer) => {
        rtiByEmployer.get(employerKey) match {
          case Some(rtiSameEmployer) => matchEmploymentsFromSameEmployer(npsSameEmployer, rtiSameEmployer)
          case None => Map.empty[NpsEmployment, RtiEmployment]
        }
      }
    }.flatten.toMap
  }

}
