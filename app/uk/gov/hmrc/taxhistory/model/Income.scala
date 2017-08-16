/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.model

import play.api.libs.json.{Format, JsString, JsSuccess, JsValue}





object IabdType extends Enumeration {
  type IabdType = Value
  val EmployerProvidedServices,
      CarFuelBenefit,
      MedicalInsurance,
      CarBenefit,
      TelephoneBenefit,
      ServiceBenefit,
      TaxableExpenseBenefit,
      VanBenefit,
      VanFuelBenefit,
      BeneficialLoan,
      Accommodation,
      Assets,
      AssetTransfer,
      EducationalService,
      Entertaining,
      ExpensesPay,
      Mileage,
      NonQualifyingRelocationExpense,
      NurseryPlaces,
      OtherItems,
      PaymentEmployeesBehalf,
      PersonalIncidentExpenses,
      QualifyingRelocationExpenses,
      EmployerProvidedProfessionalSubscription,
      IncomeTaxPaidNotDeductedFromDirectorsRemuneration,
      TravelAndSubsistence,
      VoucherAndCreditCards,
      NonCashBenefit
      = Value
  val IabdTypes = Map(
    8  -> EmployerProvidedServices,
    29 -> CarFuelBenefit,
    30 -> MedicalInsurance,
    31 -> CarBenefit,
    32 -> TelephoneBenefit,
    33 -> ServiceBenefit,
    34 -> TaxableExpenseBenefit,
    35 -> VanBenefit,
    36 -> VanFuelBenefit,
    37 -> BeneficialLoan,
    38 -> Accommodation,
    39 -> Assets,
    40 -> AssetTransfer,
    41 -> EducationalService,
    42 -> Entertaining,
    43 -> ExpensesPay,
    44 -> Mileage,
    45 -> NonQualifyingRelocationExpense,
    46 -> NurseryPlaces,
    47 -> OtherItems,
    48 -> PaymentEmployeesBehalf,
    49 -> PersonalIncidentExpenses,
    50 -> QualifyingRelocationExpenses,
    51 -> EmployerProvidedProfessionalSubscription,
    52 -> IncomeTaxPaidNotDeductedFromDirectorsRemuneration,
    53 -> TravelAndSubsistence,
    54 -> VoucherAndCreditCards,
    117-> NonCashBenefit
  )

  implicit val format = new Format[IabdType] {
    def reads(json: JsValue) = JsSuccess(IabdTypes.get(json.as[Int]).get)
    def writes(enum: IabdType) = JsString(enum.toString)
  }

}