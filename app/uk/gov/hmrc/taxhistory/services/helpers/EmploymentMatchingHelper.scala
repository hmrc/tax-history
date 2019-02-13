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

  def matchOnPayrollId(npsEmployment: NpsEmployment, rtiEmployment: RtiEmployment): Boolean = {
    (for {
      currentPayId <- rtiEmployment.currentPayId
      worksNumber  <- npsEmployment.worksNumber
    } yield {
      currentPayId == worksNumber
    }).getOrElse {
      rtiEmployment.sequenceNo == npsEmployment.sequenceNumber
    }
  }

  private def matchRti(nps: NpsEmployment, rti: List[RtiEmployment]): Map[NpsEmployment,RtiEmployment] = {
    rti.filter(matchOnPayrollId(nps, _)) match {
      case hd :: Nil => Map(nps -> hd)
      case _ => Map[NpsEmployment, RtiEmployment]()
    }
  }

  private def matchEmploymentsFromSameEmployer(nps: List[NpsEmployment], rti: List[RtiEmployment]): Map[NpsEmployment, RtiEmployment] = {
    if (nps.length == 1 && rti.length == 1) {
      Map(nps.head -> rti.head)
    }
    else {
      nps.flatMap(n => matchRti(n, rti)).toMap
    }
  }

   def matchEmployments(npsEmployments: List[NpsEmployment], rtiEmployments: List[RtiEmployment]): Map[NpsEmployment, RtiEmployment] = {
    val npsByEmployer = npsEmployments.groupBy(nps => (nps.taxDistrictNumber, nps.payeNumber))
    val rtiByEmployer = rtiEmployments.groupBy(rti => (rti.officeNumber, rti.payeRef))

    npsByEmployer.collect{
      case (k,v)  => rtiByEmployer.get(k)
        .fold(Map[NpsEmployment,RtiEmployment]())(matchEmploymentsFromSameEmployer(v, _))
    }.flatten.toMap
  }
}
