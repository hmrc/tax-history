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

package uk.gov.hmrc.taxhistory.services

import java.util.UUID

import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.model.api.{Allowance, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class AllowancesServiceSpec extends PlaySpec with MockitoSugar with TestUtil {

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()

  val testEmploymentHistoryService = TestEmploymentHistoryService.createNew()

  val npsEmploymentResponse :List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), false, false,
      Some(new LocalDate("2015-01-21")), None, true, Live))

  lazy val iabdsResponse = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]

  lazy val testNpsTaxAccount: NpsTaxAccount = loadFile("/json/nps/response/GetTaxAccount.json").as[NpsTaxAccount]
  lazy val testRtiData: RtiData = loadFile("/json/rti/response/dummyRti.json").as[RtiData]


  "Allowances" should {
    "successfully populated from iabds" in {
      when(testEmploymentHistoryService.squidNpsConnector.getEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.desNpsConnector.getIabds(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(iabdsResponse))
      when(testEmploymentHistoryService.desNpsConnector.getTaxAccount(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(testNpsTaxAccount)))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(testRtiData))
      val response =  await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino,TaxYear(2016)))

      val allowances = response.allowances
      allowances.size mustBe 1
    }

    "successfully retrieve allowance from cache" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      val allowance:List[Allowance] = List(Allowance(UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"), "payeType", 12))

      testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye)

      val result = await(testEmploymentHistoryService.getAllowances(Nino("AA000000A"), TaxYear(2014)))
      result must be(allowance)
    }

    "return no allowance from cache for current year" in {
      val result = await(testEmploymentHistoryService.getAllowances(Nino("AA000000A"), TaxYear.current))
      result must be(List.empty)
    }

  }
}
