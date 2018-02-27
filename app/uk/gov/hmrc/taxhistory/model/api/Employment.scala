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

package uk.gov.hmrc.taxhistory.model.api

import java.util.UUID

import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.time.TaxYear

case class Employment(employmentId: UUID = UUID.randomUUID(),
                      startDate: LocalDate,
                      endDate: Option[LocalDate] = None,
                      payeReference: String,
                      employerName: String,
                      companyBenefitsURI: Option[String] = None,
                      payAndTaxURI: Option[String] = None,
                      employmentURI: Option[String] = None,
                      receivingOccupationalPension: Boolean = false,
                      receivingJobSeekersAllowance: Boolean = false,
                      employmentStatus: EmploymentStatus,
                      worksNumber: String) {

  def enrichWithURIs(taxYear: Int): Employment = {
    val baseURI = s"/$taxYear/employments/${employmentId.toString}"
    this.copy(employmentURI = Some(baseURI),
      companyBenefitsURI = Some(baseURI + "/company-benefits"),
      payAndTaxURI = Some(baseURI + "/pay-and-tax"))
  }
}

object Employment {

  def noRecord(startDate: LocalDate, endDate: Option[LocalDate]): Employment = {
    val noRecord = "No record"

    // Override the end date to be None if it represents the end of the current tax year
    val overriddenEndDate =
      if (endDate.getOrElse(TaxYear.taxYearFor(startDate).finishes).equals(TaxYear.current.finishes)) {
        None
      } else {
      endDate
    }

    Employment(startDate = startDate, endDate = overriddenEndDate, payeReference = noRecord, employerName = noRecord,
      employmentStatus = EmploymentStatus.Unknown, worksNumber = noRecord)

  }

  implicit val jsonReads: Reads[Employment] = (
    (JsPath \ "employmentId").read[UUID] and
      (JsPath \ "startDate").read[LocalDate] and
      (JsPath \ "endDate").readNullable[LocalDate] and
      (JsPath \ "payeReference").read[String] and
      (JsPath \ "employerName").read[String] and
      (JsPath \ "companyBenefitsURI").readNullable[String] and
      (JsPath \ "payAndTaxURI").readNullable[String] and
      (JsPath \ "employmentURI").readNullable[String] and
      (JsPath \ "receivingOccupationalPension").read[Boolean] and
      (JsPath \ "receivingJobSeekersAllowance").read[Boolean] and
      JsPath.read[EmploymentStatus] and
      (JsPath \ "worksNumber").read[String]
    ) (Employment.apply _)


  implicit val locationWrites: Writes[Employment] = (
    (JsPath \ "employmentId").write[UUID] and
      (JsPath \ "startDate").write[LocalDate] and
      (JsPath \ "endDate").writeNullable[LocalDate] and
      (JsPath \ "payeReference").write[String] and
      (JsPath \ "employerName").write[String] and
      (JsPath \ "companyBenefitsURI").writeNullable[String] and
      (JsPath \ "payAndTaxURI").writeNullable[String] and
      (JsPath \ "employmentURI").writeNullable[String] and
      (JsPath \ "receivingOccupationalPension").write[Boolean] and
      (JsPath \ "receivingJobSeekersAllowance").write[Boolean] and
      JsPath.write[EmploymentStatus] and
      (JsPath \ "worksNumber").write[String]
    ) (unlift(Employment.unapply))

}
