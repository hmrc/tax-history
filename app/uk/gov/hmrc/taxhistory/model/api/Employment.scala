/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.libs.json.Json

case class Employment(employmentId:UUID = UUID.randomUUID(),
                      startDate:LocalDate,
                      endDate:Option[LocalDate] = None,
                      payeReference:String,
                      employerName:String,
                      companyBenefitsURI:Option[String] = None,
                      payAndTaxURI:Option[String] = None,
                      employmentURI:Option[String] = None){

  def enrichWithURIs(taxYear:Int):Employment = {
    val baseURI = s"/$taxYear/employments/${employmentId.toString}"
    this.copy(employmentURI = Some(baseURI),
              companyBenefitsURI = Some(baseURI + "/company-benefits"),
              payAndTaxURI=Some(baseURI + "/pay-and-tax"))
  }


}

object Employment {
  implicit val formats = Json.format[Employment]
}
