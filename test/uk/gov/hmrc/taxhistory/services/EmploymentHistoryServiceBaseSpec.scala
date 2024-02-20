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
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.model.api._
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.nps.{EmploymentStatus, Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.rti.RtiData
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestEmploymentHistoryService, TestUtil}
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import scala.concurrent.Future

trait EmploymentHistoryServiceBaseSpec extends DateUtils with TestUtil {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val taxYear: Int               = 2016
  val taxYear2: Int              = 2014
  val testNino: Nino             = randomNino()

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
      receivingOccupationalPension = true,
      Live
    )
  )

  val npsEmploymentWithJobSeekerAllowanceCY: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000",
      1,
      "531",
      "J4816",
      "Aldi",
      Some("6044041000000"),
      receivingJobSeekersAllowance = true,
      otherIncomeSourceIndicator = false,
      Some(LocalDate.of(TaxYear.current.currentYear, JANUARY, DAY_21)),
      None,
      receivingOccupationalPension = false,
      Live
    )
  )

  val npsEmploymentWithJobSeekerAllowanceCYMinus1: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000",
      1,
      "531",
      "J4816",
      "Aldi",
      Some("6044041000000"),
      receivingJobSeekersAllowance = true,
      otherIncomeSourceIndicator = false,
      Some(LocalDate.of(TaxYear.current.previous.currentYear, JANUARY, DAY_21)),
      None,
      receivingOccupationalPension = false,
      Live
    )
  )

  val npsEmploymentWithOtherIncomeSourceIndicator: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000",
      1,
      "531",
      "J4816",
      "Aldi",
      Some("6044041000000"),
      receivingJobSeekersAllowance = false,
      otherIncomeSourceIndicator = true,
      Some(LocalDate.of(YEAR_2015, JANUARY, DAY_21)),
      None,
      receivingOccupationalPension = false,
      Live
    )
  )

  val npsEmploymentWithJustJobSeekerAllowance: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000",
      1,
      "531",
      "J4816",
      "Aldi",
      Some("6044041000000"),
      receivingJobSeekersAllowance = true,
      otherIncomeSourceIndicator = false,
      Some(LocalDate.of(YEAR_2015, JANUARY, DAY_21)),
      None,
      receivingOccupationalPension = false,
      Live
    )
  )

  val npsEmploymentWithJustOtherIncomeSourceIndicator: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000",
      1,
      "531",
      "J4816",
      "Aldi",
      Some("6044041000000"),
      receivingJobSeekersAllowance = false,
      otherIncomeSourceIndicator = true,
      Some(LocalDate.of(YEAR_2015, JANUARY, DAY_21)),
      None,
      receivingOccupationalPension = false,
      Live
    )
  )

  lazy val testRtiData: RtiData                            = loadFile("/json/rti/response/dummyRti.json").as[RtiData]
  lazy val rtiDuplicateEmploymentsResponse: JsValue        = loadFile("/json/rti/response/dummyRtiDuplicateEmployments.json")
  lazy val rtiPartialDuplicateEmploymentsResponse: JsValue = loadFile(
    "/json/rti/response/dummyRtiPartialDuplicateEmployments.json"
  )
  lazy val rtiNonMatchingEmploymentsResponse: JsValue      = loadFile(
    "/json/rti/response/dummyRtiNonMatchingEmployment.json"
  )
  lazy val testNpsTaxAccount: NpsTaxAccount                = loadFile("/json/nps/response/GetTaxAccount.json").as[NpsTaxAccount]
  lazy val testIabds: List[Iabd]                           = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]

  val startDate: LocalDate = LocalDate.of(YEAR_2015, JANUARY, DAY_21)

  lazy val employment1: Employment = Employment(
    payeReference = "1234",
    startDate = Some(LocalDate.of(YEAR_2016, OCTOBER, DAY_20)),
    employerName = "AnEmployerName",
    employmentStatus = EmploymentStatus.Live,
    worksNumber = "00191048716"
  )

  lazy val employment2: Employment = Employment(
    payeReference = "4321",
    startDate = Some(LocalDate.of(YEAR_2015, DECEMBER, DAY_1)),
    employerName = "AnotherEmployerName",
    employmentStatus = EmploymentStatus.Live,
    worksNumber = "00191048716"
  )

  lazy val taxAccount: TaxAccount = TaxAccount(
    underpaymentAmount = Some(BigDecimal(11.11)),
    outstandingDebtRestriction = Some(BigDecimal(22.22)),
    actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal(33.33))
  )

  lazy val companyBenefit: CompanyBenefit =
    CompanyBenefit(
      iabdType = "type",
      amount = BigDecimal(123.00),
      source = None,
      captureDate = Some("5/4/2022"),
      taxYear = TaxYear(2022)
    )

  lazy val payAndTax: PayAndTax = PayAndTax(
    taxablePayTotal = Some(BigDecimal(2222.22)),
    taxablePayTotalIncludingEYU = Some(BigDecimal(2222.22)),
    taxTotal = Some(BigDecimal(111.11)),
    taxTotalIncludingEYU = Some(BigDecimal(111.11)),
    earlierYearUpdates = Nil
  )

  val testTaxCode = "1150L"

  lazy val testIncomeSource: IncomeSource = IncomeSource(1, 1, None, Nil, Nil, testTaxCode, None, 1, "")

  val payAsYouEarn: PayAsYouEarn = PayAsYouEarn(
    employments = List(employment1),
    allowances = Nil,
    incomeSources = Map(employment1.employmentId.toString -> testIncomeSource),
    benefits = Map(employment1.employmentId.toString -> List(companyBenefit)),
    payAndTax = Map(employment1.employmentId.toString -> payAndTax),
    taxAccount = None
  )

  def stubNpsGetEmploymentsSucceeds(npsEmployments: List[NpsEmployment]): OngoingStubbing[Future[List[NpsEmployment]]] =
    when(testEmploymentHistoryService.desNpsConnector.getEmployments(any(), any()))
      .thenReturn(Future.successful(npsEmployments))

  def stubRtiGetEmploymentsSucceeds(rtiEmployments: Option[RtiData]): OngoingStubbing[Future[Option[RtiData]]] =
    when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(any(), any()))
      .thenReturn(Future.successful(rtiEmployments))

  def stubNpsGetTaxAccountSucceeds(
    optTaxAccount: Option[NpsTaxAccount]
  ): OngoingStubbing[Future[Option[NpsTaxAccount]]] =
    when(testEmploymentHistoryService.desNpsConnector.getTaxAccount(any(), any()))
      .thenReturn(Future.successful(optTaxAccount))

  def stubNpsGetIabdsSucceeds(iabds: List[Iabd]): OngoingStubbing[Future[List[Iabd]]] =
    when(testEmploymentHistoryService.desNpsConnector.getIabds(any(), any()))
      .thenReturn(Future.successful(iabds))

  def stubNpsGetEmploymentsFails(failure: Throwable): OngoingStubbing[Future[List[NpsEmployment]]] =
    when(testEmploymentHistoryService.desNpsConnector.getEmployments(any(), any()))
      .thenReturn(Future.failed(failure))

  def stubNpsGetTaxAccountFails(failure: Throwable): OngoingStubbing[Future[Option[NpsTaxAccount]]] =
    when(testEmploymentHistoryService.desNpsConnector.getTaxAccount(any(), any()))
      .thenReturn(Future.failed(failure))

  def stubNpsGetIabdFails(failure: Throwable): OngoingStubbing[Future[List[Iabd]]] =
    when(testEmploymentHistoryService.desNpsConnector.getIabds(any(), any()))
      .thenReturn(Future.failed(failure))

  def stubRtiGetEmploymentsFails(failure: Throwable): OngoingStubbing[Future[Option[RtiData]]] =
    when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(any(), any()))
      .thenReturn(Future.failed(failure))

  class StubConnectors(
    npsGetEmployments: => OngoingStubbing[Future[List[NpsEmployment]]] = stubNpsGetEmploymentsSucceeds(
      npsEmploymentResponse
    ),
    npsGetTaxAccount: => OngoingStubbing[Future[Option[NpsTaxAccount]]] = stubNpsGetTaxAccountSucceeds(
      Some(testNpsTaxAccount)
    ),
    npsGetIabdDetails: => OngoingStubbing[Future[List[Iabd]]] = stubNpsGetIabdsSucceeds(testIabds),
    rti: => OngoingStubbing[Future[Option[RtiData]]] = stubRtiGetEmploymentsSucceeds(Some(testRtiData))
  ) {
    npsGetEmployments
    npsGetTaxAccount
    npsGetIabdDetails
    rti
  }

}
