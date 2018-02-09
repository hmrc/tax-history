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

package uk.gov.hmrc.taxhistory.model.nps

import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.fixtures.RtiEmployments
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class RtiEmploymentSpec extends TestUtil with UnitSpec with RtiEmployments {

  "RtiEmployment" should {

    "convert itself to PayAndTax" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val payAndTax = rtiData.employments.head.toPayAndTax
      payAndTax.taxablePayTotal should be (Some(rtiERTaxablePayTotal))
      payAndTax.taxTotal should be (Some(rtiERTaxTotal))
      payAndTax.earlierYearUpdates.size should be (1)
    }

  }

}
