/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.fixtures

import org.joda.time.LocalDate
import play.api.libs.json.JsValue
import uk.gov.hmrc.taxhistory.model.nps.{EmploymentStatus, NpsEmployment}
import uk.gov.hmrc.taxhistory.utils.TestUtil

trait NpsEmployments extends TestUtil {

  lazy val npsEmploymentResponse: JsValue =  loadFile("/json/nps/response/employment.json")
  lazy val npsTaxAccountResponse: JsValue = loadFile("/json/nps/response/GetTaxAccount.json")

  val npsEmployment1: NpsEmployment = NpsEmployment(randomNino.toString(),1,"offNo1","ref1","empname1",worksNumber = None,
    receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = false, startDate = Some(LocalDate.now()), endDate = None,
    receivingOccupationalPension = false, EmploymentStatus.Live)

  val npsEmployment2: NpsEmployment = NpsEmployment(randomNino.toString(),2,"offNo2","ref2","empname2",worksNumber = None,
    receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = false, startDate = Some(LocalDate.now()), endDate = None,
    receivingOccupationalPension = false, EmploymentStatus.Live)

  val npsEmployment3: NpsEmployment = NpsEmployment(randomNino.toString(),3,"offNo3","ref3","empname3",worksNumber = None,
    receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = false, startDate = Some(LocalDate.now()), endDate = None,
    receivingOccupationalPension = false, EmploymentStatus.Live)

}
