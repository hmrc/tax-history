package uk.gov.hmrc.taxhistory.model.nps

import play.api.libs.json.{JsResult, JsValue, Json, Reads}

case class NpsTaxAccount(sequenceNumber:Int,
                         outstandingDebtRestriction: Option[BigDecimal],
                         underpaymentAmount: Option[BigDecimal],
                         actualPUPCodedInCYPlusOneTaxYear: Option[BigDecimal])

object NpsTaxAccount {
  implicit val reader = new Reads[NpsTaxAccount] {
    def reads(js: JsValue): JsResult[NpsTaxAccount] = ???
  }
  implicit val writer = Json.writes[NpsTaxAccount]
}

