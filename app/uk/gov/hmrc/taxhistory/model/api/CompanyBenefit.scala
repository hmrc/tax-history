/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.util.UUID

case class CompanyBenefit(
  companyBenefitId: UUID = UUID.randomUUID(),
  iabdType: String,
  amount: BigDecimal,
  source: Option[Int] = None,
  captureDate: Option[String],
  taxYear: TaxYear
) {

  private def removeLeadingZeros(from: String): String = {
    val removeLeadingZeros = "^0+(?!$)"
    from.replaceFirst(removeLeadingZeros, "")
  }

  def isForecastBenefit: Boolean = {
    val April6thTaxYearStartDay = taxYear.starts

    captureDate.exists { date =>
      val captureLocalDate = date.split("/").toSeq match {
        case Seq(day, month, year) =>
          LocalDate.of(year.toInt, removeLeadingZeros(month).toInt, removeLeadingZeros(day).toInt)
        case _                     =>
          throw new IllegalArgumentException(
            s"Invalid date format: $date, expected format is dd/MM/yyyy => 1/1/2021 or 01/01/2021"
          )
      }
      // we want the day the cutoff to be 5th April for a given tax year
      captureLocalDate.isBefore(April6thTaxYearStartDay)
    }
  }
}

object CompanyBenefit {

  given formatTaxYear: OFormat[TaxYear] = Json.format[TaxYear]

  given reads: Reads[CompanyBenefit] = (
    (JsPath \ "companyBenefitId").read[UUID] and
      (JsPath \ "iabdType").read[String] and
      (JsPath \ "amount").read[BigDecimal] and
      (JsPath \ "source").readNullable[Int] and
      (JsPath \ "captureDate").readNullable[String] and
      (JsPath \ "taxYear").read[TaxYear]
  )(CompanyBenefit.apply)

  given writes: Writes[CompanyBenefit] = (cb: CompanyBenefit) =>
    Json.toJson(cb)(using Json.writes[CompanyBenefit]).as[JsObject] + ("isForecastBenefit" -> JsBoolean(
      cb.isForecastBenefit
    ))

  given formats: Format[CompanyBenefit] = Format[CompanyBenefit](reads, writes)
}
