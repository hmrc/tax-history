package uk.gov.hmrc.taxhistory.model.api

import java.util.UUID

import play.api.libs.json.Json

case class Allowance(allowanceId:UUID = UUID.randomUUID(),
                     iabdType: String,
                     amount: BigDecimal)

object Allowance {
  implicit val formats = Json.format[Allowance]
}
