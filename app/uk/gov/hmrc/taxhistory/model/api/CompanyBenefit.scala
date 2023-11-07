/*
 * Copyright 2023 HM Revenue & Customs
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

import java.util.UUID

import play.api.libs.json._

case class CompanyBenefit(
  companyBenefitId: UUID = UUID.randomUUID(),
  iabdType: String,
  amount: BigDecimal,
  source: Option[Int] = None
) {
  def isForecastBenefit: Boolean = {
    val P11D_ECS         = 19
    val P11D_Manual      = 21
    val P11D_Assessed    = 28
    val P11D_P9D_Amended = 29

    val isP11D     =
      source.contains(P11D_ECS) || source.contains(P11D_Manual) || source.contains(P11D_Assessed) || source.contains(
        P11D_P9D_Amended
      )
    val isForecast = !isP11D

    isForecast
  }
}

object CompanyBenefit {
  private val reads: Reads[CompanyBenefit] = Json.reads[CompanyBenefit]

  private val defaultWrites: Writes[CompanyBenefit] = Json.writes[CompanyBenefit]
  private val writes: Writes[CompanyBenefit]        = (cb: CompanyBenefit) => {
    Json.toJson(cb)(defaultWrites).as[JsObject] + ("isForecastBenefit" -> JsBoolean(cb.isForecastBenefit))
  }

  implicit val formats: Format[CompanyBenefit] = Format[CompanyBenefit](reads, writes)
}
