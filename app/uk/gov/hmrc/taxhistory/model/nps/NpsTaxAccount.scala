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


import java.util.UUID

import play.api.libs.json._
import uk.gov.hmrc.taxhistory.model.api.{Allowance, TaxAccount}

case class Deduction(`type`:Int,
                     npsDescription: String,
                     amount: BigDecimal,
                     sourceAmount: Option[BigDecimal])

object Deduction {
  implicit val formats = Json.format[Deduction]
}

// todo : turn the tax code into a TaxCode rather than a string, and provide JSON format for TaxCode
case class IncomeSource(employmentId:Int,
                        employmentType:Int,
                        actualPUPCodedInCYPlusOneTaxYear:Option[BigDecimal],
                        deductions: List[Deduction],
                        allowances: List[Allowance],
                        taxCode: String,
                        basisOperation: Int)

object IncomeSource {
  implicit val formats = Json.format[IncomeSource]
}

case class NpsTaxAccount(incomeSources: List[IncomeSource]){

   val PrimaryEmployment = 1
   val OutStandingDebtType = 41
   val UnderpaymentAmountType = 35

  def getPrimaryEmploymentId={
    incomeSources.find(_.employmentType==PrimaryEmployment).map(_.employmentId)
  }
  def getOutStandingDebt={
    incomeSources.find(_.employmentType==PrimaryEmployment).
      flatMap(_.deductions.find(_.`type` == OutStandingDebtType)).flatMap(_.sourceAmount)
  }

  def getUnderPayment={
    incomeSources.find(_.employmentType==PrimaryEmployment).
      flatMap(_.deductions.find(_.`type` == UnderpaymentAmountType)).flatMap(_.sourceAmount)
  }
  def getActualPupCodedInCYPlusOne={
    incomeSources.find(_.employmentType==PrimaryEmployment).flatMap(_.actualPUPCodedInCYPlusOneTaxYear)
  }

  def toTaxAccount: TaxAccount =
    TaxAccount(
      outstandingDebtRestriction = getOutStandingDebt,
      underpaymentAmount = getUnderPayment,
      actualPUPCodedInCYPlusOneTaxYear = getActualPupCodedInCYPlusOne
    )
}

object NpsTaxAccount {
  implicit val formats = Json.format[NpsTaxAccount]
}

