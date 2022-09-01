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

package uk.gov.hmrc.taxhistory.binders

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear

class PathBindersSpec extends AnyWordSpec with Matchers with OptionValues {

  private lazy val taxYear = 2018

  "ninoBinder" must {
    "parse a valid nino" in {
      PathBinders.ninoBinder.bind("someKey", "AA000003D").right.get shouldBe Nino("AA000003D")
    }

    "not parse an invalid nino" in {
      val result = PathBinders.ninoBinder.bind("someKey", "badnino").left.get
      result shouldBe "Cannot parse parameter 'someKey' with value 'badnino' as 'Nino'"
    }
  }

  "taxYearBinder" must {
    "parse a valid tax year" in {
      PathBinders.taxYearBinder.bind("someKey", "2018").right.get shouldBe TaxYear(taxYear)
    }

    "not parse an invalid tax year" in {
      val result = PathBinders.taxYearBinder.bind("someKey", "badyear").left.get
      result shouldBe "Cannot parse parameter 'someKey' with value 'badyear' as 'TaxYear'"
    }
  }
}
