/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.model.utils


import org.joda.time.LocalDate
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.time.TaxYear

import scala.annotation.tailrec
import scala.io.Source
import scala.util.Random

trait TestUtil {

  val randomNino = () => Nino(new Generator(new Random()).nextNino.value.replaceFirst("MA", "AA"))

  def loadFile(path:String): JsValue = {
    val jsonString = Source.fromURL(getClass.getResource(path)).mkString
    Json.parse(jsonString)
  }

  def loadFile(path:String, placeholders: Seq[PlaceHolder]): JsValue = {
    val jsonStringWithPlaceholders = Source.fromURL(getClass.getResource(path)).mkString
    val jsonString = replacePlaceholder(jsonStringWithPlaceholders, placeholders)
    Json.parse(jsonString)
  }

  @tailrec
  private def replacePlaceholder(string: String, pHs: Seq[PlaceHolder]): String =
    if (pHs.nonEmpty) replacePlaceholder(string.replaceAllLiterally(pHs.head.regex, pHs.head.newValue), pHs.tail) else string

  def locaDateCy (mm: String, dd: String): LocalDate = localDateInTaxYear(TaxYear.current, mm, dd)

  def locaDateCyMinus1 (mm: String, dd: String): LocalDate = localDateInTaxYear(TaxYear.current.previous, mm, dd)

  private def localDateInTaxYear (taxYear: TaxYear, mm: String, dd: String): LocalDate = {
    val startYear = new LocalDate(s"${taxYear.startYear}-$mm-$dd")
    val endYear = new LocalDate(s"${taxYear.finishYear}-$mm-$dd")
    if (TaxYear.taxYearFor(startYear) == taxYear) startYear else endYear
  }

}

case class PlaceHolder(regex: String, newValue: String)