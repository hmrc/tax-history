/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.taxhistory.model.api.{IncomeSource, TaxAccount}

case class TaDeduction(`type`: Int, npsDescription: String, amount: BigDecimal, sourceAmount: Option[BigDecimal])

object TaDeduction {
  implicit val formats: OFormat[TaDeduction] = Json.format[TaDeduction]
}

case class TaAllowance(`type`: Int, npsDescription: String, amount: BigDecimal, sourceAmount: Option[BigDecimal])

object TaAllowance {
  implicit val formats: OFormat[TaAllowance] = Json.format[TaAllowance]
}

case class NpsIncomeSource(
  employmentId: Int,
  employmentType: Option[Int],
  actualPUPCodedInCYPlusOneTaxYear: Option[BigDecimal],
  deductions: List[TaDeduction],
  allowances: List[TaAllowance],
  taxCode: Option[String],
  basisOperation: Option[Int],
  employmentTaxDistrictNumber: Option[Int],
  employmentPayeRef: Option[String]
) {
  def toIncomeSource: Option[IncomeSource] =
    if (taxCode.isEmpty || employmentType.isEmpty || employmentTaxDistrictNumber.isEmpty || employmentPayeRef.isEmpty) {
      None
    } else {
      Some(
        IncomeSource(
          employmentId = this.employmentId,
          employmentType = this.employmentType.get,
          actualPUPCodedInCYPlusOneTaxYear = this.actualPUPCodedInCYPlusOneTaxYear,
          deductions = this.deductions,
          allowances = this.allowances,
          taxCode = this.taxCode.get,
          basisOperation = this.basisOperation,
          employmentTaxDistrictNumber = this.employmentTaxDistrictNumber.get,
          employmentPayeRef = this.employmentPayeRef.get
        )
      )
    }
}

object NpsIncomeSource {
  implicit val formats: OFormat[NpsIncomeSource] = Json.format[NpsIncomeSource]
}

case class NpsTaxAccount(incomeSources: List[NpsIncomeSource]) {

  private val PrimaryEmployment      = 1
  private val OutStandingDebtType    = 41
  private val UnderpaymentAmountType = 35

  private def findPrimaryEmployment: Option[NpsIncomeSource] =
    incomeSources.find(_.employmentType.contains(PrimaryEmployment))

  private[nps] def getPrimaryEmploymentId =
    findPrimaryEmployment.map(_.employmentId)

  private[nps] def getOutStandingDebt =
    findPrimaryEmployment
      .flatMap(_.deductions.find(_.`type` == OutStandingDebtType))
      .flatMap(_.sourceAmount)

  private[nps] def getUnderPayment              =
    findPrimaryEmployment
      .flatMap(_.deductions.find(_.`type` == UnderpaymentAmountType))
      .flatMap(_.sourceAmount)
  private[nps] def getActualPupCodedInCYPlusOne =
    findPrimaryEmployment
      .flatMap(_.actualPUPCodedInCYPlusOneTaxYear)

  def toTaxAccount: TaxAccount =
    TaxAccount(
      outstandingDebtRestriction = getOutStandingDebt,
      underpaymentAmount = getUnderPayment,
      actualPUPCodedInCYPlusOneTaxYear = getActualPupCodedInCYPlusOne
    )

  def matchedIncomeSource(npsEmployment: NpsEmployment): Option[IncomeSource] = {
    val iSs = incomeSources.filter { iS =>
      val taxDistrictNumMatches =
        iS.employmentTaxDistrictNumber.map(_.toString).contains(npsEmployment.taxDistrictNumber)
      val payeRefMatches        = iS.employmentPayeRef.contains(npsEmployment.payeNumber)
      taxDistrictNumMatches && payeRefMatches
    }

    val matchedNpsIncomeSource = if (iSs.lengthCompare(1) > 0) {
      iSs.find(iS => iS.employmentId == npsEmployment.sequenceNumber)
    } else {
      iSs.headOption
    }
    matchedNpsIncomeSource.flatMap(_.toIncomeSource)
  }

}

object NpsTaxAccount {
  implicit val formats: OFormat[NpsTaxAccount] = Json.format[NpsTaxAccount]
}
