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

import org.mockito.Mockito.validateMockitoUsage
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.fixtures.{NpsEmployments, RtiEmployments}
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.RtiDataHelper
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService

class RtiDataHelperSpec extends PlaySpec with MockitoSugar with TestUtil with BeforeAndAfterEach with NpsEmployments with RtiEmployments {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val testNino = randomNino()

  "RtiDataHelper" should {
    "successfully merge if there are multiple matching rti employments for a single nps employment1 but single match on currentPayId" in {
      val rtiData = rtiPartialDuplicateEmploymentsResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]
      val testEmploymentHistoryService = TestEmploymentHistoryService.createNew()
      val matches = RtiDataHelper.normalisedEmploymentMatches(npsEmployments, rtiData.employments)

      matches.get(npsEmployments.head) must be (defined)
    }

    "return Nil constructed list if there are zero matching rti employments for a single nps employment1" in {
      val rtiData = rtiNonMatchingEmploymentsResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]
      val matches = RtiDataHelper.normalisedEmploymentMatches(npsEmployments, rtiData.employments)

      matches.get(npsEmployments.head) must be (empty)
    }

    "get pay and tax from employment1 data" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val payAndTax =RtiDataHelper.convertToPayAndTax(rtiData.employments)
      payAndTax.taxablePayTotal mustBe Some(rtiERTaxablePayTotal)
      payAndTax.taxTotal mustBe Some(rtiERTaxTotal)
      payAndTax.earlierYearUpdates.size mustBe 1
    }

    "get onlyRtiEmployments from List of Rti employments and List Nps Employments" in {
      val rtiEmployments = List(rtiEmployment1,rtiEmployment2,rtiEmployment3,rtiEmployment4)
      val npsEmployments = List(npsEmployment1,npsEmployment2,npsEmployment3)

      val onlyInRti = RtiDataHelper.employmentsOnlyInRTI(npsEmployments, rtiEmployments)

      onlyInRti.length must be (1)
      onlyInRti.head must be (rtiEmployment4)

    }

    "get onlyRtiEmployments must be size 0 when all the Rti employments are matched to the Nps Employments" in {
      val rtiEmployments = List(rtiEmployment1,rtiEmployment2,rtiEmployment3)
      val npsEmployments = List(npsEmployment1,npsEmployment2,npsEmployment3)

      val onlyInRti = RtiDataHelper.employmentsOnlyInRTI(npsEmployments, rtiEmployments)

      onlyInRti must be (empty)
    }
  }

    override protected def afterEach(): Unit = validateMockitoUsage()
  }
