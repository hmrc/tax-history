/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.taxhistory.model.rti.RtiEmployment
import uk.gov.hmrc.taxhistory.model.api.{CompanyBenefit, IncomeSource, PayAndTax, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment}
import uk.gov.hmrc.taxhistory.services.helpers.IabdsOps._
import uk.gov.hmrc.taxhistory.utils.Logging

object EmploymentHistoryServiceHelper extends TaxHistoryHelper with Logging {

  def combinePAYEs(payAsYouEarnList: List[PayAsYouEarn]): PayAsYouEarn =
    // `reduce` will cause an exception if the list of PayAsYouEarn is empty.
    payAsYouEarnList.reduce { (p1, p2) =>
      PayAsYouEarn(
        employments = p1.employments ::: p2.employments,
        benefits = p1.benefits ++ p2.benefits,
        payAndTax = p1.payAndTax ++ p2.payAndTax,
        incomeSources = p1.incomeSources ++ p2.incomeSources
      )
    }

  def buildPAYE(
    rtiEmployment: Option[RtiEmployment],
    iabds: List[Iabd],
    incomeSource: Option[IncomeSource],
    npsEmployment: NpsEmployment
  ): PayAsYouEarn = {

    val employment = npsEmployment.toEmployment

    val payAndTax: Map[String, PayAndTax] = rtiEmployment match {
      case None         => Map.empty
      case Some(rtiEmp) => Map(employment.employmentId.toString -> rtiEmp.toPayAndTax)
    }

    val benefits: Map[String, List[CompanyBenefit]] = if (iabds.nonEmpty) {
      Map(employment.employmentId.toString -> iabds.companyBenefits)
    } else {
      Map.empty
    }

    val income: Map[String, IncomeSource] = incomeSource match {
      case None     => Map.empty
      case Some(iS) => Map(employment.employmentId.toString -> iS)
    }

    PayAsYouEarn(employments = List(employment), benefits = benefits, payAndTax = payAndTax, incomeSources = income)
  }

}
