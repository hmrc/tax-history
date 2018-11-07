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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.model.api.{PayAndTax, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class PayAndTaxServiceSpec extends UnitSpec with MockitoSugar with TestUtil {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val testNino = randomNino()
  
  val testEmploymentHistoryService: EmploymentHistoryService = TestEmploymentHistoryService.createNew()

  val npsEmploymentResponse :List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), receivingJobSeekersAllowance = false,
      otherIncomeSourceIndicator = false, new LocalDate("2015-01-21"), None, receivingOccupationalPension = false, Live))


  lazy val iabdsResponse: List[Iabd] = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]

  lazy val rtiEmploymentResponse: RtiData = loadFile("/json/rti/response/dummyRti.json").as[RtiData]

  "PayAndTax" should {
    "successfully populated from rti" in {
      when(testEmploymentHistoryService.squidNpsConnector.getEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.desNpsConnector.getIabds(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(iabdsResponse))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(rtiEmploymentResponse))
      when(testEmploymentHistoryService.desNpsConnector.getTaxAccount(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))
      val payAsYouEarn = await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino,TaxYear(2016)))

      val payAndTax = payAsYouEarn.payAndTax
      payAndTax.size shouldBe 1
    }

    "successfully retrieve payAndTaxURI from cache" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]
      val testPayAndTax = Some(PayAndTax(
        payAndTaxId = UUID.fromString("2e2abe0a-8c4f-49fc-bdd2-cc13054e7172"),
        taxablePayTotal = Some(2222.22),
        taxablePayTotalIncludingEYU = Some(2222.23),
        taxTotal = Some(111.11),
        taxTotalIncludingEYU = Some(111.12),
        studentLoan = Some(333.33),
        paymentDate = Some(new LocalDate("2016-02-20")),
        earlierYearUpdates = List())
      )

      testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye)

      val payAndTax = await(testEmploymentHistoryService.getPayAndTax(Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))
      payAndTax shouldBe testPayAndTax
    }
  }

  "getAllPayAndTax" should {
    "successfully retrieve all data from cache" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]
      val testPayAndTaxList = Map("01318d7c-bcd9-47e2-8c38-551e7ccdfae3" ->
        PayAndTax(
          payAndTaxId = UUID.fromString("2e2abe0a-8c4f-49fc-bdd2-cc13054e7172"),
          taxablePayTotal = Some(2222.22),
          taxablePayTotalIncludingEYU = Some(2222.23),
          taxTotal = Some(111.11),
          taxTotalIncludingEYU = Some(111.12),
          studentLoan = Some(333.33),
          paymentDate = Some(new LocalDate("2016-02-20")),
          earlierYearUpdates = List()
        )
      )

      testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye)

      val payAndTax: Map[String, PayAndTax] = await(testEmploymentHistoryService.getAllPayAndTax(Nino("AA000000A"), TaxYear(2014)))
      payAndTax shouldBe testPayAndTaxList
    }
  }
}
