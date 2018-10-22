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

package uk.gov.hmrc.taxhistory.model.nps

import play.api.libs.json._
import uk.gov.hmrc.domain.TaxCode
import uk.gov.hmrc.domain.TaxCodeFormats._
import uk.gov.hmrc.taxhistory.model.api.TaxAccount

case class TaDeduction(`type`:Int,
                       npsDescription: String,
                       amount: BigDecimal,
                       sourceAmount: Option[BigDecimal])

object TaDeduction {
  implicit val formats = Json.format[TaDeduction]
}

case class TaAllowance(`type`:Int,
                       npsDescription: String,
                       amount: BigDecimal,
                       sourceAmount: Option[BigDecimal])

object TaAllowance {
  implicit val formats = Json.format[TaAllowance]
}

case class IncomeSource(employmentId:Int,
                        employmentType:Option[Int],
                        actualPUPCodedInCYPlusOneTaxYear:Option[BigDecimal],
                        deductions: List[TaDeduction],
                        allowances: List[TaAllowance],
                        taxCode: Option[String],
                        basisOperation: Option[Int],
                        employmentTaxDistrictNumber: Option[Int],
                        employmentPayeRef: Option[String]
                       )

object IncomeSource {
  implicit val formats = Json.format[IncomeSource]
}

case class NpsTaxAccount(incomeSources: List[IncomeSource]){

   val PrimaryEmployment = 1
   val OutStandingDebtType = 41
   val UnderpaymentAmountType = 35

  private def findPrimaryEmployment: Option[IncomeSource] = {
    incomeSources.find(_.employmentType.contains(PrimaryEmployment))
  }

  def getPrimaryEmploymentId={
    findPrimaryEmployment.map(_.employmentId)
  }
  def getOutStandingDebt={
    findPrimaryEmployment
      .flatMap(_.deductions.find(_.`type` == OutStandingDebtType)).flatMap(_.sourceAmount)
  }

  def getUnderPayment={
    findPrimaryEmployment
      .flatMap(_.deductions.find(_.`type` == UnderpaymentAmountType)).flatMap(_.sourceAmount)
  }
  def getActualPupCodedInCYPlusOne={
    findPrimaryEmployment
      .flatMap(_.actualPUPCodedInCYPlusOneTaxYear)
  }

  def toTaxAccount: TaxAccount =
    TaxAccount(
      outstandingDebtRestriction = getOutStandingDebt,
      underpaymentAmount = getUnderPayment,
      actualPUPCodedInCYPlusOneTaxYear = getActualPupCodedInCYPlusOne
    )

  def matchedIncomeSource(npsEmployment: NpsEmployment): Option[IncomeSource] = {
    val iSs = incomeSources.filter { iS =>
      val taxDistrictNumMatches = iS.employmentTaxDistrictNumber.map(_.toString).contains(npsEmployment.taxDistrictNumber)
      val payeRefMatches = iS.employmentPayeRef.contains(npsEmployment.payeNumber)
      taxDistrictNumMatches && payeRefMatches
    }

    if (iSs.lengthCompare(1) > 0) iSs.find(iS => iS.employmentId == npsEmployment.sequenceNumber) else iSs.headOption
  }

}

object NpsTaxAccount {
  implicit val formats = Json.format[NpsTaxAccount]
}

