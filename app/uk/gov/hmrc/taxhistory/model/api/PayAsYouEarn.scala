/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.model.api

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.taxhistory.model.nps.StatePension

case class PayAsYouEarn(
  employments: List[Employment] = Nil,
  allowances: List[Allowance] = Nil,
  incomeSources: Map[String, IncomeSource] = Map.empty,
  benefits: Map[String, List[CompanyBenefit]] = Map.empty,
  payAndTax: Map[String, PayAndTax] = Map.empty,
  taxAccount: Option[TaxAccount] = None,
  statePension: Option[StatePension] = None
)

object PayAsYouEarn {
  implicit val formats: OFormat[PayAsYouEarn] = Json.format[PayAsYouEarn]
}
