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


sealed trait IabdType

sealed trait CompanyBenefits extends IabdType

case object EmployerProvidedServices extends CompanyBenefits
case object CarFuelBenefit           extends CompanyBenefits
case object MedicalInsurance         extends CompanyBenefits
case object CarBenefit               extends CompanyBenefits
case object TelephoneBenefit         extends CompanyBenefits
case object ServiceBenefit           extends CompanyBenefits
case object TaxableExpenseBenefit    extends CompanyBenefits
case object VanBenefit               extends CompanyBenefits
case object VanFuelBenefit           extends CompanyBenefits
case object BeneficialLoan           extends CompanyBenefits

sealed trait BenefitInKind extends CompanyBenefits

case object TotalBenefitInKind                                extends BenefitInKind
case object Accommodation                                     extends BenefitInKind
case object Assets                                            extends BenefitInKind
case object AssetTransfer                                     extends BenefitInKind
case object EducationalService                                extends BenefitInKind
case object Entertaining                                      extends BenefitInKind
case object ExpensesPay                                       extends BenefitInKind
case object Mileage                                           extends BenefitInKind
case object NonQualifyingRelocationExpense                    extends BenefitInKind
case object NurseryPlaces                                     extends BenefitInKind
case object OtherItems                                        extends BenefitInKind
case object PaymentEmployeesBehalf                            extends BenefitInKind
case object PersonalIncidentExpenses                          extends BenefitInKind
case object QualifyingRelocationExpenses                      extends BenefitInKind
case object EmployerProvidedProfessionalSubscription          extends BenefitInKind
case object IncomeTaxPaidNotDeductedFromDirectorsRemuneration extends BenefitInKind
case object TravelAndSubsistence                              extends BenefitInKind
case object VoucherAndCreditCards                             extends BenefitInKind
case object NonCashBenefit                                    extends BenefitInKind

sealed trait Allowances extends IabdType

case object FlatRateJobExpenses       extends Allowances
case object ProfessionalSubscriptions extends Allowances
case object EarlierYearsAdjustment    extends Allowances

case object UnKnown extends IabdType


object IabdType {
  val UnknownIabdTypeId = -1000

  def apply(id: Int): IabdType = idMap.getOrElse(id, UnKnown)
  def unapply(iabdType: IabdType): Int = idMap.collectFirst({ case (key, value) if value == iabdType => key }).getOrElse(UnknownIabdTypeId)

  val idMap: Map[Int, IabdType] = Map(
    8   -> EmployerProvidedServices,
    29  -> CarFuelBenefit,
    30  -> MedicalInsurance,
    31  -> CarBenefit,
    32  -> TelephoneBenefit,
    33  -> ServiceBenefit,
    34  -> TaxableExpenseBenefit,
    35  -> VanBenefit,
    36  -> VanFuelBenefit,
    37  -> BeneficialLoan,
    28  -> TotalBenefitInKind,
    38  -> Accommodation,
    39  -> Assets,
    40  -> AssetTransfer,
    41  -> EducationalService,
    42  -> Entertaining,
    43  -> ExpensesPay,
    44  -> Mileage,
    45  -> NonQualifyingRelocationExpense,
    46  -> NurseryPlaces,
    47  -> OtherItems,
    48  -> PaymentEmployeesBehalf,
    49  -> PersonalIncidentExpenses,
    50  -> QualifyingRelocationExpenses,
    51  -> EmployerProvidedProfessionalSubscription,
    52  -> IncomeTaxPaidNotDeductedFromDirectorsRemuneration,
    53  -> TravelAndSubsistence,
    54  -> VoucherAndCreditCards,
    117 -> NonCashBenefit,
    56  -> FlatRateJobExpenses,
    57  -> ProfessionalSubscriptions,
    101 -> EarlierYearsAdjustment
  )

  implicit val format = new Format[IabdType] {
    def reads(json: JsValue) = JsSuccess(IabdType.apply(json.as[Int]))
    def writes(iabdType: IabdType) = JsNumber(IabdType.unapply(iabdType))
  }

}


case class Iabd(nino: String,
                employmentSequenceNumber: Option[Int] = None,
                `type`: IabdType,
                grossAmount : Option[BigDecimal] = None,
                typeDescription : Option[String] = None,
                source: Option[Int] = None)

object Iabd {
  implicit val formats = Json.format[Iabd]
}