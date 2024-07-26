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

package uk.gov.hmrc.taxhistory.services

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.taxhistory.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.TaxHistoryHelper

class TaxHistoryHelperSpec extends PlaySpec with TestUtil {

  object TestTaxHistoryHelper extends TaxHistoryHelper

  "TaxHistory Helper" should {

    "correctly compare matching numerical taxDistrictNumbers" in {
      TestTaxHistoryHelper.formatString("12") mustBe "12"
    }
    "correctly compare matching alphabetical taxDistrictNumbers" in {
      TestTaxHistoryHelper.formatString("ABC") mustBe "ABC"
    }

    "correctly compare taxDistrictNumbers as integers if one has a leading zero" in {
      TestTaxHistoryHelper.formatString("073") mustBe "73"
    }
    "not match different taxDistrictNumbers" in {
      TestTaxHistoryHelper.formatString("330") mustBe "330"
    }
    "not match taxDistrictNumbers if one is blank" in {
      TestTaxHistoryHelper.formatString("") mustBe ""
    }

  }
}
