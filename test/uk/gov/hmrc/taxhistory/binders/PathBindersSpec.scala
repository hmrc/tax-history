/*
 * Copyright 2004 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear

class PathBindersSpec extends AnyWordSpec with Matchers with OptionValues with EitherValues {

  private lazy val validTaxYearInt: Int           = 2018
  private lazy val invalidTaxYearAsString: String = "badyear"
  private lazy val validNinoAsString: String      = "AA000003D"
  private lazy val invalidNinoAsString: String    = "badnino"
  private lazy val formKey: String                = "someKey"

  "ninoBinder" must {
    "parse a valid nino" in {
      PathBinders.ninoBinder.bind(formKey, validNinoAsString).value shouldBe Nino(validNinoAsString)
    }

    "not parse an invalid nino" in {
      val result = PathBinders.ninoBinder.bind(formKey, invalidNinoAsString).left.value
      result shouldBe s"Cannot parse parameter '$formKey' with value '$invalidNinoAsString' as 'Nino'"
    }

    "unbind a valid nino" in {
      PathBinders.ninoBinder.unbind(formKey, Nino(validNinoAsString)) shouldBe validNinoAsString
    }
  }

  "taxYearBinder" must {
    "parse a valid tax year" in {
      PathBinders.taxYearBinder.bind(formKey, validTaxYearInt.toString).value shouldBe TaxYear(validTaxYearInt)
    }

    "not parse an invalid tax year" in {
      val result = PathBinders.taxYearBinder.bind(formKey, invalidTaxYearAsString).left.value
      result shouldBe s"Cannot parse parameter '$formKey' with value '$invalidTaxYearAsString' as 'TaxYear'"
    }

    "unbind a valid tax year" in {
      PathBinders.taxYearBinder.unbind(formKey, TaxYear(validTaxYearInt)) shouldBe validTaxYearInt.toString
    }
  }
}
