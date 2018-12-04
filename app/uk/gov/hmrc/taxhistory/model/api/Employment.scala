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
import play.api.libs.json.{__, Json, Reads, Writes}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.taxhistory.model.api.EmploymentPaymentType._
import uk.gov.hmrc.time.TaxYear

case class Employment(employmentId: UUID = UUID.randomUUID(),
                      startDate: Option[LocalDate],
                      endDate: Option[LocalDate] = None,
                      payeReference: String,
                      employerName: String,
                      companyBenefitsURI: Option[String] = None,
                      payAndTaxURI: Option[String] = None,
                      employmentURI: Option[String] = None,
                      employmentPaymentType: Option[EmploymentPaymentType] = None,
                      employmentStatus: EmploymentStatus,
                      worksNumber: String) {

  def isOccupationalPension: Boolean = employmentPaymentType.contains(OccupationalPension)
  def isJobseekersAllowance: Boolean = employmentPaymentType.contains(JobseekersAllowance)

  def enrichWithURIs(taxYear: Int): Employment = {
    val baseURI = s"/$taxYear/employments/${employmentId.toString}"
    this.copy(employmentURI = Some(baseURI),
      companyBenefitsURI = Some(baseURI + "/company-benefits"),
      payAndTaxURI = Some(baseURI + "/pay-and-tax"))
  }
}

object Employment {

  def noRecord(startDate: LocalDate, endDate: Option[LocalDate]): Employment = {
    val noRecord = "No record held"

    // Override the end date to be None if it represents the end of the current tax year
    val overriddenEndDate =
      if (endDate.getOrElse(TaxYear.taxYearFor(startDate).finishes).equals(TaxYear.current.finishes)) {
        None
      } else {
      endDate
    }

    Employment(
      startDate = Some(startDate),
      endDate = overriddenEndDate,
      payeReference = noRecord,
      employerName = noRecord,
      employmentPaymentType = None,
      employmentStatus = EmploymentStatus.Unknown,
      worksNumber = noRecord
    )
  }

  implicit val jsonReads: Reads[Employment] = (
    (__ \ "employmentId").read[UUID] and
      (__ \ "startDate").readNullable[LocalDate] and
      (__ \ "endDate").readNullable[LocalDate] and
      (__ \ "payeReference").read[String] and
      (__ \ "employerName").read[String] and
      (__ \ "companyBenefitsURI").readNullable[String] and
      (__ \ "payAndTaxURI").readNullable[String] and
      (__ \ "employmentURI").readNullable[String] and
      (__ \ "employmentPaymentType").readNullable[EmploymentPaymentType] and
      __.read[EmploymentStatus] and
      (__ \ "worksNumber").read[String]
    ) (Employment.apply _)


  implicit val jsonWrites: Writes[Employment] = (
    (__ \ "employmentId").write[UUID] and
      (__ \ "startDate").writeNullable[LocalDate] and
      (__ \ "endDate").writeNullable[LocalDate] and
      (__ \ "payeReference").write[String] and
      (__ \ "employerName").write[String] and
      (__ \ "companyBenefitsURI").writeNullable[String] and
      (__ \ "payAndTaxURI").writeNullable[String] and
      (__ \ "employmentURI").writeNullable[String] and
      (__ \ "employmentPaymentType").writeNullable[EmploymentPaymentType] and
      __.write[EmploymentStatus] and
      (__ \ "worksNumber").write[String]
    ) (unlift(Employment.unapply))

}
