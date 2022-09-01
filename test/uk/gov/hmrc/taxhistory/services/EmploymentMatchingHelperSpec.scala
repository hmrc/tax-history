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

package uk.gov.hmrc.taxhistory.services

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.model.rti.RtiData
import uk.gov.hmrc.taxhistory.fixtures.{NpsEmployments, RtiEmployments}
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentMatchingHelper

class EmploymentMatchingHelperSpec
    extends PlaySpec
    with TestUtil
    with BeforeAndAfterEach
    with NpsEmployments
    with RtiEmployments {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "EmploymentMatchingHelper" should {
    "successfully merge if there are multiple matching rti employments for a single nps employment1" when {
      val rtiData        = rtiPartialDuplicateEmploymentsResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]

      "there is a single match between the nps 'worksNumber' and rti 'currentPayId'" in {
        val matches = EmploymentMatchingHelper.matchEmployments(npsEmployments, rtiData.employments)

        matches.get(npsEmployments.head) must be(defined)
      }

      "the nps 'worksNumber' is null and rti's 'currentPayId' field is missing, " +
        "but there is a single match on nps 'sequenceNumber' and rti 'sequenceNumber'" in {
          val npsWithoutWorksNumber  = npsEmployments.map(_.copy(worksNumber = None))
          val rtiWithoutCurrentPayId = rtiData.employments.map { employment =>
            employment.copy(
              currentPayId = None,
              sequenceNo = if (employment.sequenceNo == 49) 1 else employment.sequenceNo
            )
          }

          npsWithoutWorksNumber.count(_.sequenceNumber == 1) must be(1)
          rtiWithoutCurrentPayId.count(_.sequenceNo == 1)    must be(1)

          val matches = EmploymentMatchingHelper.matchEmployments(npsWithoutWorksNumber, rtiWithoutCurrentPayId)

          matches.size must be(1)
          val optMatchingRtiEmpl = matches.get(npsWithoutWorksNumber.head)
          optMatchingRtiEmpl                must be(defined)
          optMatchingRtiEmpl.get.sequenceNo must be(1)
        }
    }

    "return Nil constructed list if there are zero matching rti employments for a single nps employment1" in {
      val rtiData        = rtiNonMatchingEmploymentsResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]
      val matches        = EmploymentMatchingHelper.matchEmployments(npsEmployments, rtiData.employments)

      matches.get(npsEmployments.head) must be(empty)
    }
  }
}
