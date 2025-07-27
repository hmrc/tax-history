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

package uk.gov.hmrc.taxhistory.model.nps

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.taxhistory.model.api.{Employment, EmploymentPaymentType}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}

class NpsEmploymentSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues with DateUtils {

  val hipNpsEmployment: NpsEmployment = NpsEmployment(
    nino = "testNino",
    sequenceNumber = 1,
    taxDistrictNumber = "abc",
    payeNumber = "1234",
    employerName = "Aldi",
    worksNumber = None,
    startDate = None,
    endDate = None,
    employmentStatus = Live
  )

  "HIPNpsEmployment methods" should {
    "work properly" in {
      hipNpsEmployment.payeRef shouldBe "abc/1234"

      val emp = hipNpsEmployment.toEmployment
      emp                       shouldBe a[Employment]
      emp.employerName          shouldBe "Aldi"
      emp.payeReference         shouldBe "abc/1234"
      emp.startDate             shouldBe None
      emp.endDate               shouldBe None
      emp.employmentPaymentType shouldBe EmploymentPaymentType.paymentType(
        "abc/1234",
        receivingOccupationalPension = false,
        receivingJobSeekersAllowance = false
      )
      emp.employmentStatus      shouldBe Live
      emp.worksNumber           shouldBe ""
    }
  }
}
