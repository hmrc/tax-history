/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.taxhistory.model.api.{Allowance, CompanyBenefit}
import uk.gov.hmrc.taxhistory.model.nps._
import uk.gov.hmrc.taxhistory.utils.Logging

/**
  * Enriches a `List[Iabd]` with various convenience methods.
  */
object IabdsOps {

  implicit class IabdListOps(val iabds: List[Iabd]) extends TaxHistoryHelper with Logging {

    def matchedCompanyBenefits(npsEmployment: NpsEmployment): List[Iabd] = iabds.filter { iabd =>
      iabd.`type`.isInstanceOf[CompanyBenefits] &&
      iabd.employmentSequenceNumber.contains(npsEmployment.sequenceNumber)
    }

    def companyBenefits: List[CompanyBenefit] = {

      def convertToCompanyBenefits(iabds: List[Iabd]): List[CompanyBenefit] =
        iabds.map { iabd =>
          CompanyBenefit(
            amount = iabd.grossAmount.getOrElse(BigDecimal(0)),
            iabdType = iabd.`type`.toString,
            source = iabd.source
          )
        }

      if (isTotalBenefitInKind) {
        convertToCompanyBenefits(this.iabds)
      } else {
        convertToCompanyBenefits(iabds.filter { iabd =>
          !iabd.`type`.isInstanceOf[TotalBenefitInKind.type]
        })
      }
    }

    /**
      * Returns true if there is one single benefit in kind and its type is [[TotalBenefitInKind]].
      */
    def isTotalBenefitInKind: Boolean = {
      val benefitsInKind = iabds.map(_.`type`).filter {
        case _: BenefitInKind => true
        case _                => false
      }
      benefitsInKind == TotalBenefitInKind :: Nil
    }

    def allowances: List[Allowance] = iabds.collect {
      case iabd if iabd.`type`.isInstanceOf[Allowances] =>
        Allowance(
          amount = iabd.grossAmount.getOrElse(BigDecimal(0)),
          iabdType = iabd.`type`.toString
        )
    }
  }

}
