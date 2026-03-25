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

package uk.gov.hmrc.taxhistory.utils

import java.time.LocalDate
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.time.TaxYear
import scala.annotation.tailrec
import scala.io.Source
import scala.util.Random

trait TestUtil {

  val randomNino: () => Nino = () => Nino(new Generator(new Random()).nextNino.value.replaceFirst("MA", "AA"))

  private def readFileAsString(path: String): String = {
    val source  = Source.fromURL(getClass.getResource(path))
    val content =
      try source.mkString
      finally source.close()
    content
  }

  def loadFile(path: String): JsValue =
    Json.parse(readFileAsString(path))

  def loadFile(path: String, placeholders: Seq[PlaceHolder]): JsValue = {
    val jsonStringWithPlaceholders = readFileAsString(path)
    val jsonString                 = replacePlaceholder(jsonStringWithPlaceholders, placeholders)
    Json.parse(jsonString)
  }

  @tailrec
  private def replacePlaceholder(string: String, pHs: Seq[PlaceHolder]): String =
    if (pHs.nonEmpty) {
      replacePlaceholder(string.replace(pHs.head.regex, pHs.head.newValue), pHs.tail)
    } else {
      string
    }

  def locaDateCyMinus1(mm: Int, dd: Int): LocalDate = localDateInTaxYear(TaxYear.current.previous, mm, dd)

  private def localDateInTaxYear(taxYear: TaxYear, mm: Int, dd: Int): LocalDate = {
    val startYear = LocalDate.of(taxYear.startYear, mm, dd)
    val endYear   = LocalDate.of(taxYear.finishYear, mm, dd)
    if (TaxYear.taxYearFor(startYear) == taxYear) startYear else endYear
  }

  val config: Map[String, Any] = Map(
    "appName"                                      -> "appName",
    "mongoExpiry"                                  -> (60 * 30),
    "desEnv"                                       -> "local",
    "desAuth"                                      -> "local",
    "mongoName"                                    -> "mongodb.name",
    "desBaseUrl"                                   -> "http://localhost:9998",
    "citizenDetailsBaseUrl"                        -> "citizen-details",
    "microservice.services.des.authorizationToken" -> "local",
    "microservice.services.des.env"                -> "local",
    "feature.isUsingHIP"                           -> "true",
    "microservice.services.hip.originator-id"      -> "testId"
  )

}
