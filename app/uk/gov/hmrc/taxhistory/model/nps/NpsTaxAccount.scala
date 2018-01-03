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

