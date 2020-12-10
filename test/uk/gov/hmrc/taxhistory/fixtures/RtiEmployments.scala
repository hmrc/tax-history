/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.JsValue
import uk.gov.hmrc.tai.model.rti.RtiEmployment
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

trait RtiEmployments extends TestUtil {

  lazy val rtiEmploymentResponse: JsValue = loadFile("/json/rti/response/dummyRti.json")
  val rtiERTaxablePayTotal: BigDecimal = BigDecimal.valueOf(20000.00)
  val rtiERTaxTotal: BigDecimal = BigDecimal.valueOf(1880.00)

  lazy val rtiPartialDuplicateEmploymentsResponse: JsValue = loadFile("/json/rti/response/dummyRtiPartialDuplicateEmployments.json")
  lazy val rtiNonMatchingEmploymentsResponse: JsValue = loadFile("/json/rti/response/dummyRtiNonMatchingEmployment.json")

  val rtiEmployment1: RtiEmployment = RtiEmployment(1,"offNo1","ref1",currentPayId = None, payments = Nil, earlierYearUpdates = Nil)
  val rtiEmployment2: RtiEmployment = RtiEmployment(5,"offNo2","ref2",currentPayId = None, payments = Nil, earlierYearUpdates = Nil)
  val rtiEmployment3: RtiEmployment = RtiEmployment(3,"offNo3","ref3",currentPayId = None, payments = Nil, earlierYearUpdates = Nil)
  val rtiEmployment4: RtiEmployment = RtiEmployment(4,"offNo4","ref4",currentPayId = None, payments = Nil, earlierYearUpdates = Nil)

}
