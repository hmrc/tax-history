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
import uk.gov.hmrc.taxhistory.model.api.{Allowance, CompanyBenefit}
import uk.gov.hmrc.taxhistory.model.nps._
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger

class IabdsHelper(val iabds:List[Iabd]) extends TaxHistoryHelper with TaxHistoryLogger{



  def getRawCompanyBenefits():List[Iabd] = {
    fetchFilteredList(this.iabds){
      iabd => {
        iabd.`type`.isInstanceOf[CompanyBenefits]
      }
    }
  }

  def getMatchedCompanyBenefits(npsEmployment: NpsEmployment):List[Iabd] = {
    fetchFilteredList(getRawCompanyBenefits()){
      (iabd) => {
        iabd.employmentSequenceNumber.contains(npsEmployment.sequenceNumber)
      }
    }
  }


  def getCompanyBenefits():List[CompanyBenefit] = {
    if (isTotalBenefitInKind()) {
      convertToCompanyBenefits(this.iabds)
    }else{
      convertToCompanyBenefits(fetchFilteredList(this.iabds) {
        iabd => {
          !iabd.`type`.isInstanceOf[TotalBenefitInKind.type]
        }
      }
      )
    }
  }

  private def convertToCompanyBenefits(iabds:List[Iabd]):List[CompanyBenefit]= {
    iabds.map {
      iabd =>
        CompanyBenefit(
          amount = iabd.grossAmount.fold {
            logger.warn("Iabds grossAmount is blank")
            BigDecimal(0)
          }(x => x),
          iabdType = iabd.`type`.toString,
          source = iabd.source
        )
    }
  }

  def isTotalBenefitInKind():Boolean = {
    fetchFilteredList(this.iabds) {
      iabd => {
        iabd.`type`.isInstanceOf[TotalBenefitInKind.type]
      }
    }.nonEmpty &&
      fetchFilteredList(this.iabds) {
        iabd => {
          iabd.`type`.isInstanceOf[uk.gov.hmrc.taxhistory.model.nps.BenefitInKind]
        }
      }.size == 1
  }


  def getRawAllowances():List[Iabd] = {

    fetchFilteredList(iabds){
      iabd => {
        iabd.`type`.isInstanceOf[Allowances]
      }
    }
  }


  def getAllowances():List[Allowance] = {
    getRawAllowances() map {
      iabd => Allowance(
        amount = iabd.grossAmount.fold{
          logger.warn("Iabds grossAmount is blank")
          BigDecimal(0)
        }(x=>x),
        iabdType = iabd.`type`.toString)
    }
  }


}
