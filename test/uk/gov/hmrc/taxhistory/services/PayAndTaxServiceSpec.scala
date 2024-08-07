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

package uk.gov.hmrc.taxhistory.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.taxhistory.model.api.{PayAndTax, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.rti.RtiData
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestEmploymentHistoryService, TestUtil}
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Await.result
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class PayAndTaxServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ScalaFutures
    with TestUtil
    with DateUtils {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val testNino: Nino             = randomNino()

  private val taxYear1 = 2014
  private val taxYear2 = 2016

  val testEmploymentHistoryService: EmploymentHistoryService = TestEmploymentHistoryService.createNew()

  val npsEmploymentResponse: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000",
      1,
      "531",
      "J4816",
      "Aldi",
      Some("6044041000000"),
      receivingJobSeekersAllowance = false,
      otherIncomeSourceIndicator = false,
      Some(LocalDate.of(YEAR_2015, JANUARY, DAY_21)),
      None,
      receivingOccupationalPension = false,
      Live
    )
  )

  val npsInvalidEmploymentResponse: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000",
      1,
      "531",
      "TEST",
      "Aldi",
      Some("6044041000000"),
      receivingJobSeekersAllowance = false,
      otherIncomeSourceIndicator = false,
      Some(LocalDate.of(YEAR_2015, JANUARY, DAY_21)),
      None,
      receivingOccupationalPension = false,
      Live
    )
  )

  lazy val iabdsResponse: List[Iabd] = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]

  lazy val rtiEmploymentResponse: RtiData = loadFile("/json/rti/response/dummyRti.json").as[RtiData]

  lazy val testNpsTaxAccount: NpsTaxAccount = loadFile("/json/nps/response/GetTaxAccount.json").as[NpsTaxAccount]

  "PayAndTax" should {
    "successfully populated from rti" in {
      when(testEmploymentHistoryService.desNpsConnector.getEmployments(any(), any()))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.desNpsConnector.getIabds(any(), any()))
        .thenReturn(Future.successful(iabdsResponse))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(any(), any()))
        .thenReturn(Future.successful(Some(rtiEmploymentResponse)))
      when(testEmploymentHistoryService.desNpsConnector.getTaxAccount(any(), any()))
        .thenReturn(Future.successful(Some(testNpsTaxAccount)))
      val payAsYouEarn = testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(taxYear2)).futureValue

      val payAndTax = payAsYouEarn.payAndTax
      payAndTax.size shouldBe 1
    }

    "successfully retrieve payAndTaxURI from cache" in {
      lazy val paye     = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]
      val testPayAndTax = Some(
        PayAndTax(
          payAndTaxId = UUID.fromString("2e2abe0a-8c4f-49fc-bdd2-cc13054e7172"),
          taxablePayTotal = Some(2222.22),
          taxablePayTotalIncludingEYU = Some(2222.23),
          taxTotal = Some(111.11),
          taxTotalIncludingEYU = Some(111.12),
          studentLoan = Some(333.33),
          paymentDate = Some(LocalDate.of(YEAR_2016, FEBRUARY, DAY_20)),
          earlierYearUpdates = List()
        )
      )

      testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(taxYear1)), paye)

      val payAndTax = testEmploymentHistoryService
        .getPayAndTax(Nino("AA000000A"), TaxYear(taxYear1), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3")
        .futureValue
      payAndTax shouldBe testPayAndTax
    }

    "failed to retrieve data from cache" in {
      val employmentId = "unknown"
      val nino         = Nino("AA000000A")
      val taxYear      = TaxYear(taxYear1)
      val exception    = intercept[NotFoundException] {
        result(
          testEmploymentHistoryService.getPayAndTax(nino, taxYear, employmentId),
          Duration.Inf
        )
      }

      exception.message shouldBe s"PayAndTax not found for NINO ${nino.value}, tax year ${taxYear.toString} and employmentId $employmentId"
    }
  }

  "getAllPayAndTax" should {
    "successfully retrieve all data from cache" in {
      lazy val paye         = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]
      val testPayAndTaxList = Map(
        "01318d7c-bcd9-47e2-8c38-551e7ccdfae3" ->
          PayAndTax(
            payAndTaxId = UUID.fromString("2e2abe0a-8c4f-49fc-bdd2-cc13054e7172"),
            taxablePayTotal = Some(2222.22),
            taxablePayTotalIncludingEYU = Some(2222.23),
            taxTotal = Some(111.11),
            taxTotalIncludingEYU = Some(111.12),
            studentLoan = Some(333.33),
            paymentDate = Some(LocalDate.of(YEAR_2016, FEBRUARY, DAY_20)),
            earlierYearUpdates = List()
          )
      )

      testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(taxYear1)), paye)

      val payAndTax: Map[String, PayAndTax] =
        testEmploymentHistoryService.getAllPayAndTax(Nino("AA000000A"), TaxYear(taxYear1)).futureValue
      payAndTax shouldBe testPayAndTaxList
    }

    "failed to retrieve data from cache" in {
      when(testEmploymentHistoryService.desNpsConnector.getEmployments(any(), any()))
        .thenReturn(Future.successful(npsInvalidEmploymentResponse))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(any(), any()))
        .thenReturn(Future.successful(Some(rtiEmploymentResponse)))

      val invalidTaxYear = 3366
      val nino           = Nino("AA000000A")
      val taxYear        = TaxYear(invalidTaxYear)
      val exception      = intercept[NotFoundException] {
        result(testEmploymentHistoryService.getAllPayAndTax(nino, taxYear), Duration.Inf)
      }

      exception.message shouldBe s"PayAndTax not found for NINO ${nino.value}, tax year ${taxYear.toString}"
    }
  }
}
