package uk.gov.hmrc.taxhistory.model.api

import java.util.UUID

import play.api.libs.json.Json

case class CompanyBenefit(companyBenefitId:UUID = UUID.randomUUID(),
                     iabdType: String,
                     amount: BigDecimal)

object CompanyBenefit {
  implicit val formats = Json.format[CompanyBenefit]
}
