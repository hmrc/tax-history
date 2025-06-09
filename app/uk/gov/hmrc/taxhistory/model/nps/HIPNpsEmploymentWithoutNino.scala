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

import java.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.taxhistory.model.utils.JsonUtils

case class HIPNpsEmploymentWithoutNino(
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
) {}

object HIPNpsEmploymentWithoutNino {
  private def withPadding(taxDistrictNumber: String): String =
    taxDistrictNumber.reverse.padTo(3, '0').reverse.mkString

  implicit val reader: Reads[HIPNpsEmploymentWithoutNino]   = (js: JsValue) => {
    val employerReference = (js \ "employerReference").validate[String].getOrElse("")
    //check substring functions
    var taxDistrictNumber = ""
    var payeNumber        = ""
    if (employerReference.contains("/")) {
      taxDistrictNumber = withPadding(employerReference.substring(0, employerReference.indexOf("/")))
      payeNumber = employerReference.substring(employerReference.indexOf("/") + 1)
    }
//TODO: what if employerReference does not contain /
    for {
      sequenceNumber               <- (js \ "employmentSequenceNumber").validate[Int]
      employerName                 <- (js \ "payeSchemeOperatorName").validate[String]
      worksNumber                  <- (js \ "worksNumber").validateOpt[String]
      receivingJobSeekersAllowance <- (js \ "jobSeekersAllowance").validate[Boolean]
      otherIncomeSourceIndicator   <- (js \ "otherIncomeSource").validate[Boolean]
      receivingOccupationalPension <- (js \ "activeOccupationalPension").validate[Boolean]
      employmentStatus             <- js.validate[EmploymentStatus]
      startDate                    <- (js \ "startDate").validateOpt[LocalDate](JsonUtils.hipnpsDateFormat)
      endDate                      <- (js \ "endDate").validateOpt[LocalDate](JsonUtils.hipnpsDateFormat)
      //employmentStatus with values Live =1, PotentiallyCeased=2, Ceased=3, PermanentlyCeased=6
    } yield HIPNpsEmploymentWithoutNino(
      sequenceNumber = sequenceNumber,
      taxDistrictNumber = taxDistrictNumber,
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
  implicit val writer: OWrites[HIPNpsEmploymentWithoutNino] = Json.writes[HIPNpsEmploymentWithoutNino]
}
