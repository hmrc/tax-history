/*
 * Copyright 2019 HM Revenue & Customs
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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear

object PathBinders {
  implicit val ninoBinder = new SimpleObjectBinder[Nino](Nino.apply, _.value)
  implicit val taxYearBinder = new SimpleObjectBinder[TaxYear](yearStr => TaxYear(yearStr.toInt), _.startYear.toString)
}