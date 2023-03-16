/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.LocalDate
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{OptionValues, PrivateMethodTester}
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.taxhistory.utils.TestUtil

class FillerStateSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues with PrivateMethodTester {
  "FillerState" when {
    "encompassed is called" should {
      val encompassed: PrivateMethod[FillerState] = PrivateMethod[FillerState](Symbol("encompassed"))

      "return Some(EncompassedEmployment)" when {
        "the filler has the same start and end days as the employment" in {
          val start = LocalDate.parse("2010-10-10")
          val end   = LocalDate.parse("2010-10-11")
          FillerState invokePrivate encompassed(start, end, start, end) shouldBe Some(EncompassedByEmployment)
        }
        "the filler has a start and end day both lying within the employment" in {
          val emplStart   = LocalDate.parse("2010-10-10")
          val emplEnd     = LocalDate.parse("2010-10-20")
          val fillerStart = emplStart.plusDays(1)
          val fillerEnd   = emplEnd.minusDays(1)
          FillerState invokePrivate encompassed(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe Some(
            EncompassedByEmployment
          )
        }
        "the employment and filler's start and end days all lie on the same day" in {
          val date = LocalDate.parse("2010-10-10")
          FillerState invokePrivate encompassed(date, date, date, date) shouldBe Some(EncompassedByEmployment)
        }
      }

      "return None" when {
        "the filler has a start day before the employment and an end day after the employment" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplStart.minusDays(1)
          val fillerEnd   = emplEnd.plusDays(1)
          FillerState invokePrivate encompassed(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe None
        }
        "the filler has a start day before the employment and an end day lying within the employment" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplStart.minusDays(1)
          val fillerEnd   = emplEnd.minusDays(1)
          FillerState invokePrivate encompassed(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe None
        }
        "the filler has a start day lying within the employment and an end day after the employment" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplStart.plusDays(1)
          val fillerEnd   = emplEnd.plusDays(1)
          FillerState invokePrivate encompassed(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe None
        }
        "the filler has no overlap with the employment" in {
          val emplStart   = LocalDate.parse("2010-10-10")
          val emplEnd     = LocalDate.parse("2010-10-20")
          val fillerStart = LocalDate.parse("2010-11-10")
          val fillerEnd   = LocalDate.parse("2010-11-20")
          FillerState invokePrivate encompassed(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe None
        }
      }
    }

    "overlapStart is called" should {
      val overlapStart: PrivateMethod[FillerState] = PrivateMethod[FillerState](Symbol("overlapStart"))

      "return Some(OverlapEmploymentStart)" when {
        "the filler has a start day before the employment and an end day lying within the employment" in {
          val emplStart   = LocalDate.parse("2010-10-10")
          val emplEnd     = LocalDate.parse("2010-10-20")
          val fillerStart = emplStart.minusDays(1)
          val fillerEnd   = emplEnd.minusDays(1)
          FillerState invokePrivate overlapStart(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe Some(
            OverlapEmploymentStart
          )
        }
        "the filler starts before and ends after the employment" in {
          val emplStart   = LocalDate.parse("2010-10-10")
          val emplEnd     = LocalDate.parse("2010-10-20")
          val fillerStart = emplStart.minusDays(1)
          val fillerEnd   = emplEnd.plusDays(1)
          FillerState invokePrivate overlapStart(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe Some(
            OverlapEmploymentStart
          )
        }
        "the filler starts before the employment ends on the same day as the start of the employment" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplStart.minusDays(1)
          val fillerEnd   = emplStart
          FillerState invokePrivate overlapStart(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe Some(
            OverlapEmploymentStart
          )
        }
      }

      "return None" when {
        "the filler has the same start and end days as the employment" in {
          val start = LocalDate.parse("2010-10-10")
          val end   = LocalDate.parse("2010-10-11")
          FillerState invokePrivate overlapStart(start, end, start, end) shouldBe None
        }
        "the employment and filler's start and end days all lie on the same day" in {
          val date = LocalDate.parse("2010-10-10")
          FillerState invokePrivate overlapStart(date, date, date, date) shouldBe None
        }
        "the filler has a start day lying within the employment and an end day after the employment" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplStart.plusDays(1)
          val fillerEnd   = emplEnd.plusDays(1)
          FillerState invokePrivate overlapStart(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe None
        }
        "the filler starts and ends before the employment starts" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplStart.minusDays(2)
          val fillerEnd   = emplStart.minusDays(1)
          FillerState invokePrivate overlapStart(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe None
        }
      }
    }

    "overlapEnd is called" should {
      val overlapEnd: PrivateMethod[FillerState] = PrivateMethod[FillerState](Symbol("overlapEnd"))

      "return Some(OverlapEmploymentEnd)" when {
        "the filler has a start day lying within the employment and an end day after the employment" in {
          val emplStart   = LocalDate.parse("2010-10-10")
          val emplEnd     = LocalDate.parse("2010-10-20")
          val fillerStart = emplStart.plusDays(1)
          val fillerEnd   = emplEnd.plusDays(1)
          FillerState invokePrivate overlapEnd(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe Some(
            OverlapEmploymentEnd
          )
        }
        "the filler starts before and ends after the employment" in {
          val emplStart   = LocalDate.parse("2010-10-10")
          val emplEnd     = LocalDate.parse("2010-10-20")
          val fillerStart = emplStart.minusDays(1)
          val fillerEnd   = emplEnd.plusDays(1)
          FillerState invokePrivate overlapEnd(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe Some(
            OverlapEmploymentEnd
          )
        }
        "the filler starts on the same day as the end of the employment and ends after the employment" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplEnd
          val fillerEnd   = emplEnd.plusDays(1)
          FillerState invokePrivate overlapEnd(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe Some(
            OverlapEmploymentEnd
          )
        }
      }

      "return None" when {
        "the filler has the same start and end days as the employment" in {
          val start = LocalDate.parse("2010-10-10")
          val end   = LocalDate.parse("2010-10-11")
          FillerState invokePrivate overlapEnd(start, end, start, end) shouldBe None
        }
        "the employment and filler's start and end days all lie on the same day" in {
          val date = LocalDate.parse("2010-10-10")
          FillerState invokePrivate overlapEnd(date, date, date, date) shouldBe None
        }
        "the filler starts before and ends within the employment" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplStart.minusDays(1)
          val fillerEnd   = emplEnd.minusDays(1)
          FillerState invokePrivate overlapEnd(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe None
        }
        "the filler starts and ends after the employment ends" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplEnd.plusDays(1)
          val fillerEnd   = emplEnd.plusDays(2)
          FillerState invokePrivate overlapEnd(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe None
        }
      }
    }

    "overlapCompletely is called" should {
      val overlapCompletely: PrivateMethod[FillerState] = PrivateMethod[FillerState](Symbol("overlapCompletely"))

      "return Some(OverlapEmployment)" when {
        "the filler starts before and ends after the employment" in {
          val emplStart   = LocalDate.parse("2010-10-10")
          val emplEnd     = LocalDate.parse("2010-10-20")
          val fillerStart = emplStart.minusDays(1)
          val fillerEnd   = emplEnd.plusDays(1)
          FillerState invokePrivate overlapCompletely(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe Some(
            OverlapEmployment
          )
        }
      }

      "return None" when {
        "the filler has the same start and end days as the employment" in {
          val start = LocalDate.parse("2010-10-10")
          val end   = LocalDate.parse("2010-10-11")
          FillerState invokePrivate overlapCompletely(start, end, start, end) shouldBe None
        }
        "the employment and filler's start and end days all lie on the same day" in {
          val date = LocalDate.parse("2010-10-10")
          FillerState invokePrivate overlapCompletely(date, date, date, date) shouldBe None
        }
        "the filler starts and ends within the employment" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplStart.plusDays(1)
          val fillerEnd   = emplEnd.minusDays(1)
          FillerState invokePrivate overlapCompletely(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe None
        }
        "the filler starts and ends before the employment starts" in {
          val emplStart = LocalDate.parse("2010-10-10")
          val emplEnd   = LocalDate.parse("2010-10-20")

          val fillerStart = emplStart.minusDays(2)
          val fillerEnd   = emplStart.minusDays(1)
          FillerState invokePrivate overlapCompletely(fillerStart, fillerEnd, emplStart, emplEnd) shouldBe None
        }
      }
    }

  }

  "DateComparisonOps" when {
    val testDateEarlier = LocalDate.parse("2010-10-10")
    val testDateLater   = testDateEarlier.plusDays(1)

    import FillerState.DateComparisonOps

    "isEqualOrBefore is called" should {
      "return true when first date is equal to second date" in {
        testDateEarlier.isEqualOrBefore(testDateEarlier)
      }
      "return true when first date is before second date" in {
        testDateEarlier.isEqualOrBefore(testDateLater) shouldBe true
      }
      "return false when first date is after second date" in {
        testDateLater.isEqualOrBefore(testDateEarlier) shouldBe false
      }
    }
    "isEqualOrAfter is called"  should {
      "return true when first date is equal to second date" in {
        testDateEarlier.isEqualOrAfter(testDateEarlier) shouldBe true
      }
      "return true when first date is after second date" in {
        testDateLater.isEqualOrAfter(testDateEarlier) shouldBe true
      }
      "return false when first date is before second date" in {
        testDateEarlier.isEqualOrAfter(testDateLater) shouldBe false
      }
    }
  }
}
