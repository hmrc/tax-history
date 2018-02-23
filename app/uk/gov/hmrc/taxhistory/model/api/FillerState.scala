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

import uk.gov.hmrc.time.TaxYear

/**
  * The filler state describes the temporal relationship of a filler to an employment.
  */
trait FillerState

object FillerState {

  /**
    * Returns a single fillerState, in order of precedence, falling back to Unrelated if the timing of
    * employment and filler have no overlap
    */
  def fillerState(filler: Employment, employment: Employment, taxYear: TaxYear): FillerState =
    Seq(encompassed(filler, employment, taxYear),
      overlapCompletely(filler, employment, taxYear),
      overlapStart(filler, employment, taxYear),
      overlapEnd(filler, employment, taxYear),
      Some(Unrelated)).flatten.head

  /*
    The filler falls entirely within the bounds of the employment
   */
  private def encompassed(filler: Employment, employment: Employment, taxYear: TaxYear): Option[FillerState] =
    if ((filler.startDate.isEqual(employment.startDate) || filler.startDate.isAfter(employment.startDate)) &&
      (filler.endDate.getOrElse(taxYear.finishes).equals(employment.endDate.getOrElse(taxYear.finishes)) ||
        filler.endDate.getOrElse(taxYear.finishes).isBefore(employment.endDate.getOrElse(taxYear.finishes)))) {
      Some(EncompassedByEmployment)
    }
    else {
      None
    }

  /*
    The filler straddles the start of the employment
   */
  private def overlapStart(filler: Employment, employment: Employment, taxYear: TaxYear): Option[FillerState] =
    if (filler.startDate.isBefore(employment.startDate) &&
      filler.endDate.getOrElse(taxYear.finishes).isAfter(employment.startDate)) {
      Some(OverlapEmploymentStart)
    } else {
      None
    }

  /*
    The filler straddles the end of the employment
   */
  private def overlapEnd(filler: Employment, employment: Employment, taxYear: TaxYear): Option[FillerState] =
    if (filler.startDate.isBefore(employment.endDate.getOrElse(taxYear.finishes)) &&
      filler.endDate.getOrElse(taxYear.finishes).isAfter(employment.endDate.getOrElse(taxYear.finishes))){
      Some(OverlapEmploymentEnd)
    } else {
      None
    }

  /*
    The employment falls entirely within the bounds of the filler
   */
  private def overlapCompletely(filler: Employment, employment: Employment, taxYear: TaxYear): Option[FillerState] =
    if (overlapStart(filler, employment, taxYear).contains(OverlapEmploymentStart) &&
      overlapEnd(filler, employment, taxYear).contains(OverlapEmploymentEnd)) {
      Some(OverlapEmployment)
    } else {
      None
    }
}

case object EncompassedByEmployment extends FillerState

case object OverlapEmploymentStart extends FillerState

case object OverlapEmploymentEnd extends FillerState

case object OverlapEmployment extends FillerState

case object Unrelated extends FillerState

