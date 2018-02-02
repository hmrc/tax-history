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
import uk.gov.hmrc.taxhistory.model.api.PayAsYouEarn
import uk.gov.hmrc.taxhistory.model.nps._
import uk.gov.hmrc.taxhistory.services.helpers.IabdsOps._
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger

object EmploymentHistoryServiceHelper extends TaxHistoryHelper with TaxHistoryLogger {

  def combinePAYEs(payAsYouEarnList: List[PayAsYouEarn]): PayAsYouEarn = {
    // `reduce` will cause an exception if the list of PayAsYouEarn is empty.
    payAsYouEarnList.reduce((p1, p2) => {
      PayAsYouEarn(
        employments = p1.employments ::: p2.employments,
        benefits =  (p1.benefits ++ p2.benefits).reduceLeftOption((a,b)=>a++b),
        payAndTax = (p1.payAndTax ++ p2.payAndTax).reduceLeftOption((a,b)=>a++b))
    })
  }

  def buildPAYE(rtiEmploymentsOption: Option[List[RtiEmployment]],
                iabdsOption: Option[List[Iabd]],
                npsEmployment: NpsEmployment): PayAsYouEarn = {

    val employment = npsEmployment.toEmployment

    val payAndTax = if (rtiEmploymentsOption.exists(_.nonEmpty)) {
      rtiEmploymentsOption.map(emps => Map(employment.employmentId.toString -> RtiDataHelper.convertToPayAndTax(emps)))
    } else {
      None
    }

    val benefits = if (iabdsOption.exists(_.nonEmpty)) {
      iabdsOption.map(iabds => Map(employment.employmentId.toString -> iabds.companyBenefits))
    } else {
      None
    }

    PayAsYouEarn(employments = List(employment), benefits = benefits, payAndTax = payAndTax)
  }
}
