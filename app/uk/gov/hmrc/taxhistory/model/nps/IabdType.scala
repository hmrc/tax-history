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

package uk.gov.hmrc.taxhistory.model.nps


import play.api.libs.json._


sealed trait IabdType

sealed trait CompanyBenefits extends IabdType

sealed trait Allowances extends IabdType

case object UnKnown extends IabdType

case object EmployerProvidedServices extends CompanyBenefits{val id=8}
case object BenefitInKind extends CompanyBenefits{val id=28}
case object CarFuelBenefit extends CompanyBenefits{val id =29}
case object MedicalInsurance extends CompanyBenefits{val id=30}
case object CarBenefit extends CompanyBenefits{val id=31}
case object TelephoneBenefit extends CompanyBenefits{val id=32}
case object ServiceBenefit extends CompanyBenefits{val id=33}
case object TaxableExpenseBenefit extends CompanyBenefits{val id=34}
case object VanBenefit extends CompanyBenefits{val id=35}
case object VanFuelBenefit extends CompanyBenefits{val id=36}
case object BeneficialLoan extends CompanyBenefits{val id=37}
case object Accommodation extends CompanyBenefits{val id=38}
case object Assets extends CompanyBenefits{val id=39}
case object AssetTransfer extends CompanyBenefits{val id=40}
case object EducationalService extends CompanyBenefits{val id=41}
case object Entertaining extends CompanyBenefits{val id=42}
case object ExpensesPay extends CompanyBenefits{val id=43}
case object Mileage extends CompanyBenefits{val id=44}
case object NonQualifyingRelocationExpense extends CompanyBenefits{val id=45}
case object NurseryPlaces extends CompanyBenefits{val id=46}
case object OtherItems extends CompanyBenefits{val id=47}
case object PaymentEmployeesBehalf extends CompanyBenefits{val id=48}
case object PersonalIncidentExpenses extends CompanyBenefits{val id=49}
case object QualifyingRelocationExpenses extends CompanyBenefits{val id=50}
case object EmployerProvidedProfessionalSubscription extends CompanyBenefits{val id=51}
case object IncomeTaxPaidNotDeductedFromDirectorsRemuneration extends CompanyBenefits{val id=52}
case object TravelAndSubsistence extends CompanyBenefits{val id=53}
case object VoucherAndCreditCards extends CompanyBenefits{val id=54}
case object NonCashBenefit extends CompanyBenefits{val id=117}

object IabdType {

  def apply(value: Int): IabdType = value match {
    case BenefitInKind.id => BenefitInKind
    case EmployerProvidedServices.id => EmployerProvidedServices
    case CarFuelBenefit.id => CarFuelBenefit
    case MedicalInsurance.id => MedicalInsurance
    case CarBenefit.id => CarBenefit
    case TelephoneBenefit.id => TelephoneBenefit
    case ServiceBenefit.id => ServiceBenefit
    case TaxableExpenseBenefit.id => TaxableExpenseBenefit
    case VanBenefit.id => VanBenefit
    case VanFuelBenefit.id => VanFuelBenefit
    case BeneficialLoan.id => BeneficialLoan
    case Accommodation.id => Accommodation
    case Assets.id => Assets
    case AssetTransfer.id => AssetTransfer
    case EducationalService.id => EducationalService
    case Entertaining.id => Entertaining
    case ExpensesPay.id => ExpensesPay
    case Mileage.id => Mileage
    case NonQualifyingRelocationExpense.id => NonQualifyingRelocationExpense
    case NurseryPlaces.id => NurseryPlaces
    case OtherItems.id => OtherItems
    case PaymentEmployeesBehalf.id => PaymentEmployeesBehalf
    case PersonalIncidentExpenses.id => PersonalIncidentExpenses
    case QualifyingRelocationExpenses.id => QualifyingRelocationExpenses
    case EmployerProvidedProfessionalSubscription.id => EmployerProvidedProfessionalSubscription
    case IncomeTaxPaidNotDeductedFromDirectorsRemuneration.id => IncomeTaxPaidNotDeductedFromDirectorsRemuneration
    case TravelAndSubsistence.id => TravelAndSubsistence
    case VoucherAndCreditCards.id => VoucherAndCreditCards
    case NonCashBenefit.id => NonCashBenefit
    case _ => UnKnown
  }

  def unapply(value: IabdType): Int = value match {
    case BenefitInKind => BenefitInKind.id
    case EmployerProvidedServices => EmployerProvidedServices.id
    case CarFuelBenefit => CarFuelBenefit.id
    case MedicalInsurance => MedicalInsurance.id
    case CarBenefit => CarBenefit.id
    case TelephoneBenefit => TelephoneBenefit.id
    case ServiceBenefit => ServiceBenefit.id
    case TaxableExpenseBenefit => TaxableExpenseBenefit.id
    case VanBenefit => VanBenefit.id
    case VanFuelBenefit => VanFuelBenefit.id
    case BeneficialLoan => BeneficialLoan.id
    case Accommodation => Accommodation.id
    case Assets => Assets.id
    case AssetTransfer => AssetTransfer.id
    case EducationalService => EducationalService.id
    case Entertaining => Entertaining.id
    case ExpensesPay => ExpensesPay.id
    case Mileage => Mileage.id
    case NonQualifyingRelocationExpense => NonQualifyingRelocationExpense.id
    case NurseryPlaces => NurseryPlaces.id
    case OtherItems => OtherItems.id
    case PaymentEmployeesBehalf => PaymentEmployeesBehalf.id
    case PersonalIncidentExpenses => PersonalIncidentExpenses.id
    case QualifyingRelocationExpenses => QualifyingRelocationExpenses.id
    case EmployerProvidedProfessionalSubscription => EmployerProvidedProfessionalSubscription.id
    case IncomeTaxPaidNotDeductedFromDirectorsRemuneration => IncomeTaxPaidNotDeductedFromDirectorsRemuneration.id
    case TravelAndSubsistence => TravelAndSubsistence.id
    case VoucherAndCreditCards => VoucherAndCreditCards.id
    case NonCashBenefit => NonCashBenefit.id
    case UnKnown => -1000 //TODO is their better way
  }


  implicit val format = new Format[IabdType] {
    def reads(json: JsValue) = JsSuccess(IabdType.apply(json.as[Int]))
    def writes(iabdType: IabdType) = JsNumber(IabdType.unapply(iabdType))
  }

}


case class Iabd (nino: String, employmentSequenceNumber: Option[Int] = None, `type`: IabdType, grossAmount : Option[BigDecimal] = None,
                 typeDescription : Option[String] = None, source:Option[Int]=None)

object Iabd {
  implicit val formats = Json.format[Iabd]
}