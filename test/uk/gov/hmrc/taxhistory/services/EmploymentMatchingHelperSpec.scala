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

package uk.gov.hmrc.taxhistory.services

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.fixtures.{NpsEmployments, RtiEmployments}
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentMatchingHelper

class EmploymentMatchingHelperSpec extends PlaySpec with TestUtil with BeforeAndAfterEach with NpsEmployments with RtiEmployments {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val testNino = randomNino()

  "EmploymentMatchingHelper" should {
    "successfully merge if there are multiple matching rti employments for a single nps employment1" when {
      val rtiData = rtiPartialDuplicateEmploymentsResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]

      "there is a single match between the nps 'worksNumber' and rti 'currentPayId'" in {
        val matches = EmploymentMatchingHelper.matchedEmployments(npsEmployments, rtiData.employments)

        matches.get(npsEmployments.head) must be(defined)
      }

      "the nps 'worksNumber' is null and rti's 'currentPayId' field is missing, " +
        "but there is a single match on nps 'sequenceNumber' and rti 'sequenceNumber'" in {
        val npsWithoutWorksNumber = npsEmployments.map(_.copy(worksNumber = None))
        val rtiWithoutCurrentPayId = rtiData.employments.map { employment =>
          employment.copy(
            currentPayId = None,
            sequenceNo = if (employment.sequenceNo == 49) 1 else employment.sequenceNo
          )
        }

        npsWithoutWorksNumber.count(_.sequenceNumber == 1) must be(1)
        rtiWithoutCurrentPayId.count(_.sequenceNo == 1) must be(1)

        val matches = EmploymentMatchingHelper.matchedEmployments(npsWithoutWorksNumber, rtiWithoutCurrentPayId)

        matches.size must be(1)
        val optMatchingRtiEmpl = matches.get(npsWithoutWorksNumber.head)
        optMatchingRtiEmpl must be(defined)
        optMatchingRtiEmpl.get.sequenceNo must be(1)
      }
    }

    "return Nil constructed list if there are zero matching rti employments for a single nps employment1" in {
      val rtiData = rtiNonMatchingEmploymentsResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]
      val matches = EmploymentMatchingHelper.matchedEmployments(npsEmployments, rtiData.employments)

      matches.get(npsEmployments.head) must be (empty)
    }

    "get onlyRtiEmployments from List of Rti employments and List Nps Employments" in {
      val rtiEmployments = List(rtiEmployment1,rtiEmployment2,rtiEmployment3,rtiEmployment4)
      val npsEmployments = List(npsEmployment1,npsEmployment2,npsEmployment3)

      val onlyInRti = EmploymentMatchingHelper.unmatchedRtiEmployments(npsEmployments, rtiEmployments)

      onlyInRti.length must be (1)
      onlyInRti.head must be (rtiEmployment4)

    }

    "get onlyRtiEmployments must be size 0 when all the Rti employments are matched to the Nps Employments" in {
      val rtiEmployments = List(rtiEmployment1,rtiEmployment2,rtiEmployment3)
      val npsEmployments = List(npsEmployment1,npsEmployment2,npsEmployment3)

      val onlyInRti = EmploymentMatchingHelper.unmatchedRtiEmployments(npsEmployments, rtiEmployments)

      onlyInRti must be (empty)
    }
  }
}
