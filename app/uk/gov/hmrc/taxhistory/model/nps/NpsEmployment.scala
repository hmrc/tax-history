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

package uk.gov.hmrc.taxhistory.model.nps

import play.api.libs.functional.syntax.toFunctionalBuilderOps

import java.time.LocalDate
import play.api.libs.json.*
import uk.gov.hmrc.taxhistory.model.api.{Employment, EmploymentPaymentType}
import uk.gov.hmrc.taxhistory.model.utils.JsonUtils

case class NpsEmployment(
  nino: String,
  sequenceNumber: Int,
  taxDistrictNumber: String,
  payeNumber: String,
  employerName: String,
  worksNumber: Option[String] = None,
  receivingJobSeekersAllowance: Boolean = false,
  otherIncomeSourceIndicator: Boolean = false,
  startDate: Option[LocalDate],
  endDate: Option[LocalDate] = None,
  receivingOccupationalPension: Boolean = false,
  employmentStatus: EmploymentStatus
) {

  def payeRef: String = taxDistrictNumber + "/" + payeNumber

  def toEmployment: Employment = {
    val employmentPaymentType =
      EmploymentPaymentType.paymentType(payeRef, receivingOccupationalPension, receivingJobSeekersAllowance)
    Employment(
      employerName = employerName,
      payeReference = payeRef,
      startDate = startDate,
      endDate = endDate,
      employmentPaymentType = employmentPaymentType,
      employmentStatus = employmentStatus,
      worksNumber = worksNumber.getOrElse("")
    )
  }
}

object NpsEmployment {
  given reader: Reads[NpsEmployment] = (js: JsValue) => {
    val startDate = (js \ "startDate").asOpt[LocalDate](using JsonUtils.npsDateFormat)
    val endDate   = (js \ "endDate").asOpt[LocalDate](using JsonUtils.npsDateFormat)
    for {
      nino                         <- (js \ "nino").validate[String]
      sequenceNumber               <- (js \ "sequenceNumber").validate[Int]
      taxDistrictNumber            <- (js \ "taxDistrictNumber").validate[String]
      payeNumber                   <- (js \ "payeNumber").validate[String]
      employerName                 <- (js \ "employerName").validate[String]
      worksNumber                  <- (js \ "worksNumber").validateOpt[String]
      receivingJobSeekersAllowance <- (js \ "receivingJobseekersAllowance").validate[Boolean]
      otherIncomeSourceIndicator   <- (js \ "otherIncomeSourceIndicator").validate[Boolean]
      receivingOccupationalPension <- (js \ "receivingOccupationalPension").validate[Boolean]
      employmentStatus             <- js.validate[EmploymentStatus]
    } yield NpsEmployment(
      nino = nino,
      sequenceNumber = sequenceNumber,
      taxDistrictNumber = withPadding(taxDistrictNumber),
      payeNumber = payeNumber,
      employerName = employerName,
      worksNumber = worksNumber,
      receivingJobSeekersAllowance = receivingJobSeekersAllowance,
      otherIncomeSourceIndicator = otherIncomeSourceIndicator,
      startDate = startDate,
      endDate = endDate,
      receivingOccupationalPension = receivingOccupationalPension,
      employmentStatus = employmentStatus
    )
  }

  given writer: OWrites[NpsEmployment] = (
    (__ \ "nino").write[String] and
      (__ \ "sequenceNumber").write[Int] and
      (__ \ "taxDistrictNumber").write[String] and
      (__ \ "payeNumber").write[String] and
      (__ \ "employerName").write[String] and
      (__ \ "worksNumber").writeNullable[String] and
      (__ \ "receivingJobSeekersAllowance").write[Boolean] and
      (__ \ "otherIncomeSourceIndicator").write[Boolean] and
      (__ \ "startDate").writeNullable[LocalDate] and
      (__ \ " endDate").writeNullable[LocalDate] and
      (__ \ "receivingOccupationalPension").write[Boolean] and
      (__ \ "employmentStatus").write[EmploymentStatus]
  )(o => Tuple.fromProductTyped(o))

  /*
  in some seen cases taxDistrictNumber is two digits - we assume this is because the first digit was 0 and was
  dropped since the field type in the NPS API is an integer. In such cases, pad to 3 digits.
   */
  private def withPadding(taxDistrictNumber: String): String =
    taxDistrictNumber.reverse.padTo(3, '0').reverse.mkString
}
