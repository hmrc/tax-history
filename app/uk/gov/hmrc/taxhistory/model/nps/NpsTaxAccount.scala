package uk.gov.hmrc.taxhistory.model.nps

import play.api.libs.json._
import uk.gov.hmrc.tai.model.rti.{RtiEarlierYearUpdate, RtiEmployment, RtiPayment}

case class NpsTaxAccount(employmentSequenceNumber:Option[Int],
                         outstandingDebtRestriction: Option[BigDecimal],
                         underpaymentAmount: Option[BigDecimal],
                         actualPUPCodedInCYPlusOneTaxYear: Option[BigDecimal])

object NpsTaxAccount {
  implicit val reader = new Reads[NpsTaxAccount] {
    def reads(js: JsValue): JsResult[NpsTaxAccount] =
    {


        for {
          employmentSequenceNumber <- (js \ "sequenceNumber" ).validateOpt[Int]
          outstandingDebtRestriction <- (js \ "empRefs" \ "officeNo").validateOpt[BigDecimal]
          underpaymentAmount <- (js \ "empRefs" \ "payeRef").validateOpt[BigDecimal]
          actualPUPCodedInCYPlusOneTaxYear <- (js \ "currentPayId").validateOpt[BigDecimal]
        } yield {
          NpsTaxAccount(
            employmentSequenceNumber = Some(employmentSequenceNumber),
            outstandingDebtRestriction = Some(outstandingDebtRestriction),
            underpaymentAmount = Some(underpaymentAmount),
            actualPUPCodedInCYPlusOneTaxYear = Some(actualPUPCodedInCYPlusOneTaxYear)
          )
        }

    }
  }
  implicit val writer = Json.writes[NpsTaxAccount]
}

