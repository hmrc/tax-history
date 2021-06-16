/*
 * Copyright 2021 HM Revenue & Customs
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

import org.joda.time.LocalDate

/**
  * The filler state describes the temporal relationship of a filler (an employment gap) to an employment.
  */
trait FillerState

object FillerState {

  /**
    * Returns a single fillerState, in order of precedence, falling back to Unrelated if the timing of
    * employment and filler have no overlap
    */
  def fillerState(fillerStartDate: LocalDate, fillerEndDate: LocalDate, employmentStartDate: LocalDate, employmentEndDate: LocalDate): FillerState = {
    Seq(encompassed(fillerStartDate, fillerEndDate, employmentStartDate, employmentEndDate),
      overlapCompletely(fillerStartDate, fillerEndDate, employmentStartDate, employmentEndDate),
      overlapStart(fillerStartDate, fillerEndDate, employmentStartDate, employmentEndDate),
      overlapEnd(fillerStartDate, fillerEndDate, employmentStartDate, employmentEndDate),
      Some(Unrelated)).flatten.head
  }

  /*
    The filler falls entirely within the bounds of the employment
   */
  private def encompassed(fillerStartDate: LocalDate, fillerEndDate: LocalDate, employmentStartDate: LocalDate, employmentEndDate: LocalDate): Option[FillerState] =
    if (fillerStartDate.isEqualOrAfter(employmentStartDate) && fillerEndDate.isEqualOrBefore(employmentEndDate)) {
      Some(EncompassedByEmployment)
    }
    else {
      None
    }

  /*
    The filler straddles the start of the employment
   */
  private def overlapStart(fillerStartDate: LocalDate, fillerEndDate: LocalDate, employmentStartDate: LocalDate, employmentEndDate: LocalDate): Option[FillerState] =
    if (fillerStartDate.isBefore(employmentStartDate) && fillerEndDate.isEqualOrAfter(employmentStartDate)) {
      Some(OverlapEmploymentStart)
    } else {
      None
    }

  /*
    The filler straddles the end of the employment
   */
  private def overlapEnd(fillerStartDate: LocalDate, fillerEndDate: LocalDate, employmentStartDate: LocalDate, employmentEndDate: LocalDate): Option[FillerState] =
    if (fillerStartDate.isEqualOrBefore(employmentEndDate) && fillerEndDate.isAfter(employmentEndDate)){
      Some(OverlapEmploymentEnd)
    } else {
      None
    }

  /*
    The employment falls entirely within the bounds of the filler
   */
  private def overlapCompletely(fillerStartDate: LocalDate, fillerEndDate: LocalDate, employmentStartDate: LocalDate, employmentEndDate: LocalDate): Option[FillerState] = {
    val isOverlappingStart = overlapStart(fillerStartDate, fillerEndDate, employmentStartDate, employmentEndDate).contains(OverlapEmploymentStart)
    val isOverlappingEnd = overlapEnd(fillerStartDate, fillerEndDate, employmentStartDate, employmentEndDate).contains(OverlapEmploymentEnd)
    if (isOverlappingStart && isOverlappingEnd) {
      Some(OverlapEmployment)
    } else {
      None
    }
  }

  private[api] implicit class DateComparisonOps(someDate: LocalDate) {
    def isEqualOrBefore(comparedToThisDate: LocalDate) =
      someDate.isEqual(comparedToThisDate) || someDate.isBefore(comparedToThisDate)

    def isEqualOrAfter(comparedToThisDate: LocalDate) =
      someDate.isEqual(comparedToThisDate) || someDate.isAfter(comparedToThisDate)
  }
}

case object EncompassedByEmployment extends FillerState

case object OverlapEmploymentStart extends FillerState

case object OverlapEmploymentEnd extends FillerState

case object OverlapEmployment extends FillerState

case object Unrelated extends FillerState

