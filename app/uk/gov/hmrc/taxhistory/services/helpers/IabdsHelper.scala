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

import uk.gov.hmrc.taxhistory.model.api.{Allowance, CompanyBenefit}
import uk.gov.hmrc.taxhistory.model.nps._
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger

class IabdsHelper(val iabds: List[Iabd]) extends TaxHistoryHelper with TaxHistoryLogger {

  def rawCompanyBenefits: List[Iabd] = iabds.filter {
    case _: CompanyBenefits => true
    case _                  => false
  }

  def matchedCompanyBenefits(npsEmployment: NpsEmployment):List[Iabd] =
    rawCompanyBenefits.filter(_.employmentSequenceNumber.contains(npsEmployment.sequenceNumber))


  def companyBenefits: List[CompanyBenefit] = {
    if (isTotalBenefitInKind()) {
      convertToCompanyBenefits(this.iabds)
    } else {
      convertToCompanyBenefits(iabds.filter {
        iabd => {
          !iabd.`type`.isInstanceOf[TotalBenefitInKind.type]
        }
      }
      )
    }
  }

  private def convertToCompanyBenefits(iabds:List[Iabd]): List[CompanyBenefit] = {
    iabds.map { iabd =>
      CompanyBenefit(
        amount = iabd.grossAmount.getOrElse {
          logger.warn("Iabds grossAmount is blank")
          BigDecimal(0)
        },
        iabdType = iabd.`type`.toString,
        source = iabd.source
      )
    }
  }

  def isTotalBenefitInKind(): Boolean = {
    iabds.exists(_.`type`.isInstanceOf[TotalBenefitInKind.type]) &&
      iabds.count(_.`type`.isInstanceOf[uk.gov.hmrc.taxhistory.model.nps.BenefitInKind]) == 1
  }


  def rawAllowances: List[Iabd] = iabds.filter {
    case _: Allowances => true
    case _             => false
  }


  def allowances: List[Allowance] = {
    rawAllowances map { iabd =>
      Allowance(
        amount = iabd.grossAmount.getOrElse {
          logger.warn("Iabds grossAmount is blank")
          BigDecimal(0)
        },
        iabdType = iabd.`type`.toString
      )
    }
  }


}
