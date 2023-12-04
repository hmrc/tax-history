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

import play.api.libs.json._
import uk.gov.hmrc.taxhistory.utils.LocalDateHelpers
import uk.gov.hmrc.time.TaxYear

import java.util.UUID

case class CompanyBenefit(
  companyBenefitId: UUID = UUID.randomUUID(),
  iabdType: String,
  amount: BigDecimal,
  source: Option[Int] = None,
  captureDate: Option[String],
  taxYear: TaxYear
) extends LocalDateHelpers {

  def isForecastBenefit: Boolean = {
    val April6thTaxYearStartDay = taxYear.starts

    captureDate.exists { date =>
      val captureLocalDate = strDateToLocalDate(date)
      // we want the day the cutoff to be 5th April for a given tax year
      captureLocalDate.isBefore(April6thTaxYearStartDay)
    }
  }
}

object CompanyBenefit {

  implicit val formatTaxYear: OFormat[TaxYear] = Json.format[TaxYear]

  implicit val reads: Reads[CompanyBenefit] = Json.reads[CompanyBenefit]

  private val defaultWrites: Writes[CompanyBenefit] = Json.writes[CompanyBenefit]
  private val writes: Writes[CompanyBenefit]        = (cb: CompanyBenefit) => {
    Json.toJson(cb)(defaultWrites).as[JsObject] + ("isForecastBenefit" -> JsBoolean(cb.isForecastBenefit))
  }

  implicit val formats: Format[CompanyBenefit] = Format[CompanyBenefit](reads, writes)
}
