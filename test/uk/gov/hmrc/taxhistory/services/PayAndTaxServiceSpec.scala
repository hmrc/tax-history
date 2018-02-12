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

import java.util.UUID

import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.model.api.{PayAndTax, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class PayAndTaxServiceSpec extends PlaySpec with MockitoSugar with TestUtil {
  implicit val hc = HeaderCarrier()
  val testNino = randomNino()
  
  val testEmploymentHistoryService = TestEmploymentHistoryService.createNew()

  val npsEmploymentResponse :List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), false, false,
      new LocalDate("2015-01-21"), None, false, Live))


  lazy val iabdsResponse = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]

  lazy val rtiEmploymentResponse = loadFile("/json/rti/response/dummyRti.json").as[RtiData]

  "PayAndTax " should {
    "successfully populated from rti" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(iabdsResponse))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(rtiEmploymentResponse))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))
      val payAsYouEarn = await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino,TaxYear(2016)))

      val payAndTax = payAsYouEarn.payAndTax
      payAndTax.size mustBe 1
    }

    "successfully retrieve payAndTaxURI from cache" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]
      val testPayAndTax = PayAndTax(UUID.fromString("2e2abe0a-8c4f-49fc-bdd2-cc13054e7172"), Some(2222.22),Some(111.11),
        Some(new LocalDate("2016-02-20")), List())

      testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye)

      val payAndTax = await(testEmploymentHistoryService.getPayAndTax(Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))
      payAndTax must be(testPayAndTax)
    }

  }
}
