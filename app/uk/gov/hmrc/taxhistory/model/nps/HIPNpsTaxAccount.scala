/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json, OWrites, Reads}
import uk.gov.hmrc.taxhistory.model.api.{IncomeSource, TaxAccount}

case class AllowanceOrDeduction(
  `type`: Int,
  npsDescription: String,
  amount: BigDecimal,
  sourceAmount: Option[BigDecimal]
)

object AllowanceOrDeduction {
  implicit val reader: Reads[AllowanceOrDeduction]                = (js: JsValue) => {
    val typeAndDescription         = (js \ "type").validate[String].getOrElse("")
    val (npsDescription, typeCode) = typeAndDescription.split("[(]") match {
      case Array(desc, code) => (Some(desc.trim), code.substring(0, code.indexOf(")")).toIntOption)
      case _                 => (None, None)
    }
    for {
      amount       <- (js \ "adjustedAmount").validate[BigDecimal]
      sourceAmount <- (js \ "sourceAmount").validateOpt[BigDecimal]
    } yield AllowanceOrDeduction(
      `type` = typeCode.getOrElse(0),
      npsDescription = npsDescription.getOrElse(""),
      amount = amount,
      sourceAmount = sourceAmount
    )
  }
  implicit val writer: OWrites[AllowanceOrDeduction]              = Json.writes[AllowanceOrDeduction]
  //TODO: to be removed
  def toTaAllowance(allowance: AllowanceOrDeduction): TaAllowance =
    TaAllowance(allowance.`type`, allowance.npsDescription, allowance.amount, allowance.sourceAmount)
  def toTaDeduction(deduction: AllowanceOrDeduction): TaDeduction =
    TaDeduction(deduction.`type`, deduction.npsDescription, deduction.amount, deduction.sourceAmount)
}

case class HIPNpsIncomeSource(
  employmentId: Int,
  employmentType: Option[Int],
  actualPUPCodedInCYPlusOneTaxYear: Option[BigDecimal],
  deductions: List[AllowanceOrDeduction],
  allowances: List[AllowanceOrDeduction],
  taxCode: Option[String],
  basisOperation: Option[Int],
  employmentTaxDistrictNumber: Option[Int],
  employmentPayeRef: Option[String]
) {
  def toIncomeSource: Option[IncomeSource] =
    for {
      etype   <- employmentType
      code    <- taxCode
      distNum <- employmentTaxDistrictNumber
      ref     <- employmentPayeRef
    } yield IncomeSource(
      employmentId = this.employmentId,
      employmentType = etype,
      actualPUPCodedInCYPlusOneTaxYear = this.actualPUPCodedInCYPlusOneTaxYear,
      deductions = this.deductions.map(AllowanceOrDeduction.toTaDeduction),
      allowances = this.allowances.map(AllowanceOrDeduction.toTaAllowance),
      taxCode = code,
      basisOperation = this.basisOperation,
      employmentTaxDistrictNumber = distNum,
      employmentPayeRef = ref
    )
}

object HIPNpsIncomeSource {
  implicit val reader: Reads[HIPNpsIncomeSource]                                 = (js: JsValue) => {
    val employerReference                                = (js \ "employerReference").validate[String].getOrElse("")
    val (employmentTaxDistrictNumber, employmentPayeRef) = employerReference.split("/") match {
      case Array(taxDistrict, payeRef) => (taxDistrict.toIntOption, Some(payeRef))
      case _                           => (None, None)
    }
    //TODO: what if the string doesnt contain /
    val employmentType                                   = (js \ "employmentRecordType").validateOpt[String].getOrElse("") match {
      case Some("PRIMARY")   => Some(1)
      case Some("SECONDARY") => Some(2)
      case _                 => None
    }
    val basisOperation                                   = (js \ "basisOfOperation").validateOpt[String].getOrElse("") match {
      case Some("Week1/Month1")              => Some(1)
      case Some("Cumulative")                => Some(2)
      case Some("Week1/Month1,Not Operated") => Some(3)
      case Some("Cumulative,Not Operated")   => Some(4)
      case _                                 => None
    }
    for {
      employmentId                     <- (js \ "employmentSequenceNumber").validate[Int]
      actualPUPCodedInCYPlusOneTaxYear <- (js \ "actualPUPCodedInNextYear").validateOpt[BigDecimal]
      deductions                       <- (js \ "deductionsDetails").validate[List[AllowanceOrDeduction]]
      allowances                       <- (js \ "allowancesDetails").validate[List[AllowanceOrDeduction]]
      taxCode                          <- (js \ "taxCode").validateOpt[String]
    } yield HIPNpsIncomeSource(
      employmentId = employmentId,
      employmentType = employmentType,
      actualPUPCodedInCYPlusOneTaxYear = actualPUPCodedInCYPlusOneTaxYear,
      deductions = deductions,
      allowances = allowances,
      taxCode = taxCode,
      basisOperation = basisOperation,
      employmentTaxDistrictNumber = employmentTaxDistrictNumber,
      employmentPayeRef = employmentPayeRef
    )
  }
  //TODO: to be removed
  def toNpsIncomeSource(hipNpsIncomeSource: HIPNpsIncomeSource): NpsIncomeSource = NpsIncomeSource(
    hipNpsIncomeSource.employmentId,
    hipNpsIncomeSource.employmentType,
    hipNpsIncomeSource.actualPUPCodedInCYPlusOneTaxYear,
    hipNpsIncomeSource.deductions.map(AllowanceOrDeduction.toTaDeduction),
    hipNpsIncomeSource.allowances.map(AllowanceOrDeduction.toTaAllowance),
    hipNpsIncomeSource.taxCode,
    hipNpsIncomeSource.basisOperation,
    hipNpsIncomeSource.employmentTaxDistrictNumber,
    hipNpsIncomeSource.employmentPayeRef
  )
}

case class HIPNpsTaxAccount(incomeSources: List[HIPNpsIncomeSource]) {

  private val PrimaryEmployment      = 1
  private val OutStandingDebtType    = 41
  private val UnderpaymentAmountType = 35

  private def findPrimaryEmployment: Option[HIPNpsIncomeSource] =
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

object HIPNpsTaxAccount {
  implicit val reader: Reads[HIPNpsTaxAccount]                           = (js: JsValue) =>
    for {
      incomeSources <- (js \ "employmentDetailsList").validate[List[HIPNpsIncomeSource]]
    } yield HIPNpsTaxAccount(incomeSources)
  //TODO: to be removed
  def toNpsTaxAccount(hipNpsTaxAccount: HIPNpsTaxAccount): NpsTaxAccount = NpsTaxAccount(
    hipNpsTaxAccount.incomeSources.map(HIPNpsIncomeSource.toNpsIncomeSource)
  )
}
