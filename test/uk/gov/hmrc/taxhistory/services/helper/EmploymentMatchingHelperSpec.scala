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

package uk.gov.hmrc.taxhistory.services.helper

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.rti.RtiEmployment
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helper.HelperTestData.{nps, rti}
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentMatchingHelper

class EmploymentMatchingHelperSpec extends PlaySpec with MockitoSugar with TestUtil {


  "Given 2 NPS and 2 RTI employments from the same employer, match those 2 NPS employments with the RTI employments" in {

    val rtiRecords:List[RtiEmployment] =
      List(rti(currentPayId = Some("1234"), seq = 3), rti(currentPayId = Some("5678"), seq = 4))
    val npsRecords: List[NpsEmployment] =
      List(nps(worksNumber = Some("1234"), seq = 3), nps(worksNumber = Some("5678"), seq = 4))

    EmploymentMatchingHelper.matchEmployments(npsRecords, rtiRecords).size must be(2)
  }

  "Given 2 NPS and 1 RTI employments from the same employer, match the RTI to the correct NPS" in {

    val rtiRecords:List[RtiEmployment] = List(rti(currentPayId = Some("1234"), seq = 3))
    val npsRecords: List[NpsEmployment] = List(
      nps(worksNumber = Some("1234"), seq = 3),
      nps(worksNumber = Some("5678"), seq = 4))

    val result = EmploymentMatchingHelper.matchEmployments(npsRecords, rtiRecords)

    result.size must be(1)
    result(result.keySet.head).currentPayId.get must be("1234")
  }

  "Given 1 NPS and 2 RTI employments from the same employer, match the NPS to the correct RTI" in {

    val rtiRecords:List[RtiEmployment] = List(
      rti(currentPayId = Some("1234"), seq = 3),
      rti(currentPayId = Some("5678"), seq = 4))
    val npsRecords: List[NpsEmployment] = List(nps(worksNumber = Some("5678"), seq = 4))

    val result = EmploymentMatchingHelper.matchEmployments(npsRecords, rtiRecords)

    result.size must be(1)
    result(result.keySet.head).currentPayId.get must be("5678")

  }

  "Given 1 NPS and 1 RTI employment from the same employer but the pid does not match, the employments are matched," in {

    val rtiRecords:List[RtiEmployment] = List(rti(currentPayId = Some("1234"), seq =3))
    val npsRecords: List[NpsEmployment] = List(nps(worksNumber = Some("5678"), seq = 4))

    EmploymentMatchingHelper.matchEmployments(npsRecords, rtiRecords).size must be(1)
  }

  "Given 2 NPS and 2 RTI employments from the same employer but the pid does not match on one, there is one correct match" in {

    val rtiRecords:List[RtiEmployment] = List(
      rti(currentPayId = Some("1234"), seq = 3),
      rti(currentPayId = Some("5678"), seq = 4))
    val npsRecords: List[NpsEmployment] = List(
      nps(worksNumber = Some("5678"), seq = 4),
      nps(worksNumber = Some("1111"), seq = 4))

    EmploymentMatchingHelper.matchEmployments(npsRecords, rtiRecords).size must be(1)
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

  "Given NPS employment with leading 0 dropped from officeNumber, do not match with corresponding (3 digit) RTI record" in {

    val npsRecords: List[NpsEmployment] = List(
      nps(taxDistrictNumber = "12", payeNumber = "xxx123456", worksNumber = Some("1234"), seq = 3),
      nps(taxDistrictNumber = "12", payeNumber = "xxx123456", worksNumber = Some("5678"), seq = 4)
    )

    val rtiRecords: List[RtiEmployment] = List(
      rti(officeNumber = "012", payeRef = "xxx123456", currentPayId = Some("5678"), seq = 4)
    )

    val result: Map[NpsEmployment, RtiEmployment] = EmploymentMatchingHelper.matchEmployments(npsRecords, rtiRecords)

    result.isEmpty mustBe true
  }

  "Given two NPS employments from the same employer but only one RTI, the RTI will be matched on both NPS employments" +
    "if the worksNumber/currentPayId are identical on both NPS records but seq numbers are unique" in {

    val npsRecords: List[NpsEmployment] = List(
      nps(taxDistrictNumber = "012", payeNumber = "xxx123456", worksNumber = Some("5678"), seq = 3),
      nps(taxDistrictNumber = "012", payeNumber = "xxx123456", worksNumber = Some("5678"), seq = 4)
    )

    val rtiRecords: List[RtiEmployment] = List(
      rti(officeNumber = "012", payeRef = "xxx123456", currentPayId = Some("5678"), seq = 4)
    )

    val result: Map[NpsEmployment, RtiEmployment] = EmploymentMatchingHelper.matchEmployments(npsRecords, rtiRecords)

    result.size must be(2)

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
