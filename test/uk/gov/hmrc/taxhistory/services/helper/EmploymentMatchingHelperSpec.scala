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

package uk.gov.hmrc.taxhistory.services.helper

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.rti.RtiEmployment
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helper.HelperTestData.{nps, rti}
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentMatchingHelper

class EmploymentMatchingHelperSpec extends PlaySpec with MockitoSugar with TestUtil {

  "Given 2 NPS and 2 RTI employments from the same employer, match those 2 NPS employments with the RTI employments" when {
    "the worksNumber/currentPayId is present" in {
      val rtiEmployment1 = rti(currentPayId = Some("1234"), seq = 3)
      val rtiEmployment2 = rti(currentPayId = Some("5678"), seq = 4)

      val npsEmployment1 = nps(worksNumber = Some("1234"), seq = 3)
      val npsEmployment2 = nps(worksNumber = Some("5678"), seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(
        npsEmployments = List(npsEmployment1, npsEmployment2),
        rtiEmployments = List(rtiEmployment1, rtiEmployment2)
      ).toSeq

      matchedEmployments must have length 2
      matchedEmployments must contain (npsEmployment1, rtiEmployment1)
      matchedEmployments must contain (npsEmployment2, rtiEmployment2)
    }

    "the worksNumber/currentPayId is missing" in {
      val rtiEmployment1 = rti(currentPayId = None, seq = 3)
      val rtiEmployment2 = rti(currentPayId = None, seq = 4)

      val npsEmployment1 = nps(worksNumber = None, seq = 3)
      val npsEmployment2 = nps(worksNumber = None, seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(
        npsEmployments = List(npsEmployment1, npsEmployment2),
        rtiEmployments = List(rtiEmployment1, rtiEmployment2)
      ).toSeq

      matchedEmployments must have length 2
      matchedEmployments must contain (npsEmployment1, rtiEmployment1)
      matchedEmployments must contain (npsEmployment2, rtiEmployment2)
    }
  }

  "Given 2 NPS and 1 RTI employments from the same employer, match the RTI to the one correct NPS" when {
    "the two NPS employments have a different worksNumber/currentPayId" in {
      val rtiEmployment = rti(currentPayId = Some("1234"), seq = 3)

      val npsEmployment1 = nps(worksNumber = Some("1234"), seq = 3)
      val npsEmployment2 = nps(worksNumber = Some("5678"), seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(
        npsEmployments = List(npsEmployment1, npsEmployment2),
        rtiEmployments = List(rtiEmployment)
      ).toSeq

      matchedEmployments must have length 1
      matchedEmployments must contain (npsEmployment1, rtiEmployment)
    }

    "the two NPS employments have the same worksNumber/currentPayId but a different sequenceNumber" in {
      val rtiEmployment = rti(currentPayId = Some("456"), seq = 1)

      val npsEmployment1 = nps(worksNumber = Some("456"), seq = 1)
      val npsEmployment2 = nps(worksNumber = Some("456"), seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(
        npsEmployments = List(npsEmployment1, npsEmployment2),
        rtiEmployments = List(rtiEmployment)
      ).toSeq

      matchedEmployments must have length 1
      matchedEmployments must contain (npsEmployment1, rtiEmployment)
    }

    "the two NPS employments have a missing worksNumber/currentPayId" in {
      val rtiEmployment = rti(currentPayId = None, seq = 1)

      val npsEmployment1 = nps(worksNumber = None, seq = 1)
      val npsEmployment2 = nps(worksNumber = None, seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(
        npsEmployments = List(npsEmployment1, npsEmployment2),
        rtiEmployments = List(rtiEmployment)
      ).toSeq

      matchedEmployments must have length 1
      matchedEmployments must contain (npsEmployment1, rtiEmployment)
    }
  }

  "Given 1 NPS and 2 RTI employments from the same employer, match the NPS to the correct RTI" when {
    "the two RTI employments have a different worksNumber/currentPayId" in {
      val rtiEmployment1 = rti(currentPayId = Some("1234"), seq = 3)
      val rtiEmployment2 = rti(currentPayId = Some("5678"), seq = 4)

      val npsEmployment = nps(worksNumber = Some("5678"), seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(
        npsEmployments = List(npsEmployment),
        rtiEmployments = List(rtiEmployment1, rtiEmployment2)
      ).toSeq

      matchedEmployments must have length 1
      matchedEmployments must contain (npsEmployment, rtiEmployment2)
    }

    "the two RTI employments have the same worksNumber/currentPayId but different sequenceNumber" in {
      val rtiEmployment1 = rti(currentPayId = Some("5678"), seq = 3)
      val rtiEmployment2 = rti(currentPayId = Some("5678"), seq = 4)

      val npsEmployment = nps(worksNumber = Some("5678"), seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(
        npsEmployments = List(npsEmployment),
        rtiEmployments = List(rtiEmployment1, rtiEmployment2)
      ).toSeq

      matchedEmployments must have length 1
      matchedEmployments must contain (npsEmployment, rtiEmployment2)
    }

    "the two RTI employments have a missing worksNumber/currentPayId" in {
      val rtiEmployment1 = rti(currentPayId = None, seq = 3)
      val rtiEmployment2 = rti(currentPayId = None, seq = 4)

      val npsEmployment = nps(worksNumber = None, seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(
        npsEmployments = List(npsEmployment),
        rtiEmployments = List(rtiEmployment1, rtiEmployment2)
      ).toSeq

      matchedEmployments must have length 1
      matchedEmployments must contain (npsEmployment, rtiEmployment2)
    }
  }

  "Given 1 NPS and 1 RTI employment from the same employer, the employments are matched only by taxOfficeNumber/payeRef" when {
    "even if the worksNumber/currentPayId and sequence numbers do not match" in {
      val rtiEmployment = rti(currentPayId = Some("1234"), seq = 3)
      val npsEmployment = nps(worksNumber = Some("5678"), seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(List(npsEmployment), List(rtiEmployment)).toSeq

      matchedEmployments must have length 1
      matchedEmployments must contain (npsEmployment, rtiEmployment)
    }

    "even if the worksNumber/currentPayId is missing and the sequence numbers do not match" in {
      val rtiEmployment = rti(currentPayId = None, seq = 3)
      val npsEmployment = nps(worksNumber = None, seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(List(npsEmployment), List(rtiEmployment)).toSeq

      matchedEmployments must have length 1
      matchedEmployments must contain (npsEmployment, rtiEmployment)
    }
  }

  "Given 2 NPS and 2 RTI employments from the same employer but there is only one matching pair, there is one correct match" when {
    "the worksNumber/currentPayId is present but matches only on one pair" in {
      val rtiEmployment1 = rti(currentPayId = Some("1234"), seq = 3)
      val rtiEmployment2 = rti(currentPayId = Some("5678"), seq = 4)

      val npsEmployment1 = nps(worksNumber = Some("5678"), seq = 4)
      val npsEmployment2 = nps(worksNumber = Some("1111"), seq = 4)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(
        npsEmployments = List(npsEmployment1, npsEmployment2),
        rtiEmployments = List(rtiEmployment1, rtiEmployment2)
      ).toSeq

      matchedEmployments must have length 1
      matchedEmployments must contain (npsEmployment1, rtiEmployment2)
    }

    "the worksNumber/currentPayId is missing and the sequenceNumber matches only on one pair" in {
      val rtiEmployment1 = rti(currentPayId = None, seq = 3)
      val rtiEmployment2 = rti(currentPayId = None, seq = 4)

      val npsEmployment1 = nps(worksNumber = None, seq = 4)
      val npsEmployment2 = nps(worksNumber = None, seq = 5)

      val matchedEmployments = EmploymentMatchingHelper.matchEmployments(
        npsEmployments = List(npsEmployment1, npsEmployment2),
        rtiEmployments = List(rtiEmployment1, rtiEmployment2)
      ).toSeq

      matchedEmployments must have length 1
      matchedEmployments must contain (npsEmployment1, rtiEmployment2)
    }
  }

  "Given multiple same employer employments, match the employments correctly" in {

    val rtiRecords:List[RtiEmployment] = List(
      rti(officeNumber = "123", payeRef = "xxx123456", currentPayId = Some("1234"), seq = 3),
      rti(officeNumber = "888", payeRef = "xxx888888", currentPayId = Some("7654"), seq = 8),
      rti(officeNumber = "888", payeRef = "xxx888888", currentPayId = Some("7764"), seq = 9),
      rti(officeNumber = "123", payeRef = "xxx123456", currentPayId = Some("5678"), seq = 4),
      rti(officeNumber = "999", payeRef = "yyy555555", currentPayId = Some("9999"), seq = 10)
    )
    val npsRecords: List[NpsEmployment] = List(
      nps(taxDistrictNumber = "123", payeNumber = "xxx123456", worksNumber = Some("1234"), seq = 3),
      nps(taxDistrictNumber = "123", payeNumber = "xxx123456", worksNumber = Some("5678"), seq = 4),
      nps(taxDistrictNumber = "888", payeNumber = "xxx888888", worksNumber = Some("7654"), seq = 8),
      nps(taxDistrictNumber = "888", payeNumber = "xxx888888", worksNumber = Some("7764"), seq = 9),
      nps(taxDistrictNumber = "999", payeNumber = "yyy555555", worksNumber = Some("9999"), seq = 10)
    )

    val result: Map[NpsEmployment, RtiEmployment] = EmploymentMatchingHelper.matchEmployments(npsRecords, rtiRecords)

    result.size must be(5)

    result.keySet.toList.map(_.worksNumber.get).sorted must be(result.values.toList.map(_.currentPayId.get).sorted)
  }

  "Given a nonempty list of NPS employments and and empty list of RTI employments return an empty Map" in {

    val npsRecords: List[NpsEmployment] = List(
      nps(taxDistrictNumber = "123", payeNumber = "xxx123456", worksNumber = Some("1234"), seq = 3),
      nps(taxDistrictNumber = "123", payeNumber = "xxx123456", worksNumber = Some("5678"), seq = 4),
      nps(taxDistrictNumber = "888", payeNumber = "xxx888888", worksNumber = Some("7654"), seq = 8),
      nps(taxDistrictNumber = "888", payeNumber = "xxx888888", worksNumber = Some("7764"), seq = 9),
      nps(taxDistrictNumber = "999", payeNumber = "yyy555555", worksNumber = Some("9999"), seq = 10)
    )

    val rtiRecords: List[RtiEmployment] = List()

    val result: Map[NpsEmployment, RtiEmployment] = EmploymentMatchingHelper.matchEmployments(npsRecords, rtiRecords)

    result.isEmpty mustBe true
  }

  "Given a nonempty list of RTI employments and and empty list of NPS employments return an empty Map" in {

    val rtiRecords:List[RtiEmployment] = List(
      rti(officeNumber = "123", payeRef = "xxx123456", currentPayId = Some("1234"), seq = 3),
      rti(officeNumber = "888", payeRef = "xxx888888", currentPayId = Some("7654"), seq = 8),
      rti(officeNumber = "888", payeRef = "xxx888888", currentPayId = Some("7764"), seq = 9),
      rti(officeNumber = "123", payeRef = "xxx123456", currentPayId = Some("5678"), seq = 4),
      rti(officeNumber = "999", payeRef = "yyy555555", currentPayId = Some("9999"), seq = 10)
    )

    val npsRecords: List[NpsEmployment] = List()

    val result: Map[NpsEmployment, RtiEmployment] = EmploymentMatchingHelper.matchEmployments(npsRecords, rtiRecords)

    result.isEmpty mustBe true
  }


}
