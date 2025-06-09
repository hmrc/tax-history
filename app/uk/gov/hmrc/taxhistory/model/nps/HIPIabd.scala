/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.Logging
import play.api.libs.json.{JsValue, Json, OFormat, OWrites, Reads}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.matching.Regex

object IabdSource {
  private val sources: List[String]                          = List(
    "575T",
    "AGENT CONTACT",
    "Annual Coding",
    "Assessed P11D",
    "BULK EXPENSES",
    "Budget Coding",
    "CALCULATED",
    "CALCULATION ONLY",
    "ChB Online Service",
    "Cutover",
    "DWP Estimated JSA",
    "DWP Uprate",
    "EMAIL",
    "ESA",
    "FPS(RTI)",
    "FWKS",
    "Home Working Expenses",
    "Information Letter",
    "Internet",
    "Internet Calculated",
    "LETTER",
    "Lump Sum (IYC)",
    "OTHER FORM",
    "P11D (ECS)",
    "P11D (MANUAL)",
    "P11D/P9D Amended",
    "P161",
    "P161W",
    "P46(CAR) (ECS)",
    "P46(CAR) (MANUAL)",
    "P46(DWP)",
    "P46(DWP) Uprated",
    "P46(PEN)",
    "P50 CESSATION",
    "P50 UNEMPLOYMENT",
    "P52",
    "P53 (IYC)",
    "P53A",
    "P53B",
    "P810",
    "P85",
    "P87",
    "P9D",
    "Payrolling BIK",
    "R27",
    "R40",
    "R40 (IYC)",
    "SA",
    "SA Autocoding",
    "SPA AUTOCODING",
    "T&TSP",
    "TELEPHONE CALL",
    "HICBC PAYE"
  )
  def getInt(sourceText: Option[String] = None): Option[Int] = sourceText match {
    case Some(x) if sources.contains(x) => Some(sources.indexOf(x) + 1)
    case _                              => None
  }
}
object IabdPaymentFrequency {
  private val paymentFrequencies: List[String]               =
    List("WEEKLY", "QUARTERLY", "MONTHLY", "FORTNIGHTLY", "ANNUALLY", "4 WEEKLY")
  def getInt(sourceText: Option[String] = None): Option[Int] = sourceText match {
    case Some(x) if paymentFrequencies.contains(x) => Some(paymentFrequencies.indexOf(x) + 1)
    case _                                         => None
  }
}

case class HIPIabd(
  nino: String,
  employmentSequenceNumber: Option[Int] = None,
  `type`: IabdType,
  grossAmount: Option[BigDecimal] = None,
  typeDescription: Option[String] = None,
  source: Option[Int] = None,
  captureDate: Option[String],
  paymentFrequency: Option[Int] = None,
  startDate: Option[String] = None
) extends Logging {

  def toStatePension: StatePension = {
    val weeklyPaymentFrequency              = IabdPaymentFrequency.getInt(Some("WEEKLY"))
    val annualPaymentFrequency              = IabdPaymentFrequency.getInt(Some("ANNUALLY"))
    val paymentStartDate: Option[LocalDate] =
      paymentFrequency match {
        case `weeklyPaymentFrequency` =>
          startDate.map(date => LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy/MM/dd")))
        case `annualPaymentFrequency` =>
          None
        case Some(unknownValue)       =>
          logger.warn(s"[Iabd][toStatePension] Unknown value for IABD's 'paymentFrequency': $unknownValue")
          None
        case _                        =>
          None
      }

    StatePension(
      grossAmount = grossAmount.getOrElse(0.0),
      typeDescription.getOrElse(""),
      paymentFrequency = paymentFrequency,
      startDate = paymentStartDate
    )
  }

  //TODO: to be removed in code cleanup
  def toIabd: Iabd = Iabd(
    nino,
    employmentSequenceNumber,
    `type`,
    grossAmount,
    typeDescription,
    source,
    captureDate,
    paymentFrequency,
    startDate
  )
}

object HIPIabd extends Logging {
  implicit val reader: Reads[HIPIabd]   = (js: JsValue) => {
    val typeAndDescription          = (js \ "type").as[String]
    val (typeDescription, typeCode) = typeAndDescription.split("[(]") match {
      case Array(desc, code) => (Some(desc.trim), code.substring(0, code.indexOf(")")).toIntOption)
      case _                 => (None, None)
    }
    def formatDate(date: String) = {
      val dateRegex: Regex = """^(\d\d\d\d)-(\d\d)-(\d\d)$""".r
      date match {
        case dateRegex() => LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        case _           =>
          logger.error(s"Invalid date format [yyyy-MM-dd]: $date")
          ""
      }

    }
    for {
      nino                     <- (js \ "nationalInsuranceNumber").validate[String]
      employmentSequenceNumber <- (js \ "employmentSequenceNumber").validateOpt[Int]
      grossAmount              <- (js \ "grossAmount").validateOpt[BigDecimal]
      sourceText               <- (js \ "source").validateOpt[String]
      captureDate              <- (js \ "captureDate").validateOpt[String]
      paymentFrequencyString   <- (js \ "paymentFrequency").validateOpt[String]
      startDate                <- (js \ "startDate").validateOpt[String]
    } yield HIPIabd(
      nino = nino,
      `type` = IabdType.apply(typeCode.getOrElse(0)),
      typeDescription = typeDescription,
      employmentSequenceNumber = employmentSequenceNumber,
      grossAmount = grossAmount,
      source = IabdSource.getInt(sourceText),
      captureDate = captureDate.map(formatDate),
      paymentFrequency = IabdPaymentFrequency.getInt(paymentFrequencyString),
      startDate = startDate.map(formatDate)
    )
  }
  implicit val writer: OWrites[HIPIabd] = Json.writes[HIPIabd]
}

case class HIPIabdList(iabdDetails: Option[List[HIPIabd]]) {
  val getListOfIabd: List[Iabd] = iabdDetails match {
    case Some(x) => x.map(x => x.toIabd)
    case None    => List.empty
  }
}

object HIPIabdList {
  implicit val formats: OFormat[HIPIabdList] = Json.format[HIPIabdList]
}
