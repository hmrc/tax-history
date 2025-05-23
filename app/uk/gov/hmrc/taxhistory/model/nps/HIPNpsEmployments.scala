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

import play.api.libs.json.{JsValue, Reads}
case class HIPNpsEmployments(nino: String, hipNpsEmploymentsWithoutNino: List[HIPNpsEmploymentWithoutNino])

object HIPNpsEmployments {
  def toListOfHIPNpsEmployment(hipNpsEmployments: HIPNpsEmployments): List[HIPNpsEmployment] =
    hipNpsEmployments.hipNpsEmploymentsWithoutNino.map(HIPNpsEmployment(hipNpsEmployments.nino)).toList
  implicit val reader: Reads[HIPNpsEmployments]                                              = (js: JsValue) =>
    for {
      nino                             <- (js \ "nationalInsuranceNumber").validate[String]
      hipNpsEmploymentArrayWithoutNINO <-
        (js \ "individualsEmploymentDetails").validate[List[HIPNpsEmploymentWithoutNino]]
    } yield HIPNpsEmployments(nino, hipNpsEmploymentArrayWithoutNINO)
}
