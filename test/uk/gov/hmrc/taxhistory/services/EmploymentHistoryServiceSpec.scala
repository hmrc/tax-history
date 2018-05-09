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
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.fixtures.Employments
import uk.gov.hmrc.taxhistory.model.api.{CompanyBenefit, Employment, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.nps.{EmploymentStatus, Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.{PlaceHolder, TestUtil}
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentMatchingHelper
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class EmploymentHistoryServiceSpec extends UnitSpec with MockitoSugar with TestUtil with Employments {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val testNino = randomNino()

  val testEmploymentHistoryService: EmploymentHistoryService = TestEmploymentHistoryService.createNew()

  val npsEmploymentResponse: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), receivingJobSeekersAllowance = false,
      otherIncomeSourceIndicator = false, new LocalDate("2015-01-21"), None, receivingOccupationalPension = true, Live))

  val npsEmploymentWithJobSeekerAllowanceCY: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), receivingJobSeekersAllowance = true, otherIncomeSourceIndicator = false,
      new LocalDate(s"${TaxYear.current.currentYear}-01-21"), None, receivingOccupationalPension = false, Live))

  val npsEmploymentWithJobSeekerAllowanceCYMinus1: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), receivingJobSeekersAllowance = true, otherIncomeSourceIndicator = false,
      new LocalDate(s"${TaxYear.current.previous.currentYear}-01-21"), None, receivingOccupationalPension = false, Live))

  val npsEmploymentWithOtherIncomeSourceIndicator: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = true,
      new LocalDate("2015-01-21"), None, receivingOccupationalPension = false, Live))


  val npsEmploymentWithJustJobSeekerAllowance: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), receivingJobSeekersAllowance = true, otherIncomeSourceIndicator = false,
      new LocalDate("2015-01-21"), None, receivingOccupationalPension = false, Live))

  val npsEmploymentWithJustOtherIncomeSourceIndicator: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = true,
      new LocalDate("2015-01-21"), None, receivingOccupationalPension = false, Live))


  lazy val testRtiData: RtiData = loadFile("/json/rti/response/dummyRti.json").as[RtiData]
  lazy val rtiDuplicateEmploymentsResponse: JsValue = loadFile("/json/rti/response/dummyRtiDuplicateEmployments.json")
  lazy val rtiPartialDuplicateEmploymentsResponse: JsValue = loadFile("/json/rti/response/dummyRtiPartialDuplicateEmployments.json")
  lazy val rtiNonMatchingEmploymentsResponse: JsValue = loadFile("/json/rti/response/dummyRtiNonMatchingEmployment.json")
  lazy val testNpsTaxAccount: NpsTaxAccount = loadFile("/json/nps/response/GetTaxAccount.json").as[NpsTaxAccount]
  lazy val testIabds: List[Iabd] = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]

  val startDate = new LocalDate("2015-01-21")

  "Employment Service" should {
    "successfully get Nps Employments Data" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(any(), any()))
        .thenReturn(Future.successful(npsEmploymentResponse))

      noException shouldBe thrownBy(await(testEmploymentHistoryService.retrieveNpsEmployments(testNino, TaxYear(2016))))
    }

    "successfully get Nps Employments Data with jobseekers allowance for cy-1" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(any(), any()))
        .thenReturn(Future.successful(npsEmploymentWithJobSeekerAllowanceCYMinus1))

      noException shouldBe thrownBy(await(testEmploymentHistoryService.retrieveNpsEmployments(testNino, TaxYear(2016))))
    }

    "return any non success status response from get Nps Employments api" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(any(), any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      intercept[BadRequestException](await(testEmploymentHistoryService.retrieveNpsEmployments(testNino, TaxYear(2016))))
    }

    "successfully get Rti Employments Data" in {
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(any(), any()))
        .thenReturn(Future.successful(testRtiData))

      val result = await(testEmploymentHistoryService.retrieveRtiData(testNino, TaxYear(2016)))
      result shouldBe Some(testRtiData)
    }

    "return not found status response from get Nps Employments api" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(any(), any()))
        .thenReturn(Future.successful(Nil))
      intercept[NotFoundException](await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(2016))))
    }

    "return success status despite failing response from get Rti Employments api when there are nps employments" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(any(), any()))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(any(), any()))
        .thenReturn(Future.failed(new BadRequestException("")))
      when(testEmploymentHistoryService.npsConnector.getIabds(any(), any()))
        .thenReturn(Future.failed(new BadRequestException("")))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(any(), any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      noException shouldBe thrownBy {
        await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(2016)))
      }
    }

    "return success response from get Employments" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(any(), any()))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.npsConnector.getIabds(any(), any()))
        .thenReturn(Future.successful(testIabds))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(any(), any()))
        .thenReturn(Future.successful(testRtiData))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(any(), any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      val paye = await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(2016)))

      val employments = paye.employments
      employments.size shouldBe 1
      employments.head.employerName shouldBe "Aldi"
      employments.head.payeReference shouldBe "531/J4816"
      employments.head.startDate shouldBe startDate
      employments.head.endDate shouldBe None

      val Some(payAndTax) = paye.payAndTax.get(employments.head.employmentId.toString)
      payAndTax.taxablePayTotal shouldBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.taxTotal shouldBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.earlierYearUpdates.size shouldBe 1

      val eyu = payAndTax.earlierYearUpdates.head
      eyu.receivedDate shouldBe new LocalDate("2016-06-01")
      eyu.taxablePayEYU shouldBe BigDecimal(-600.99)
      eyu.taxEYU shouldBe BigDecimal(-10.99)

      val Some(statePension) = paye.statePension
      statePension.grossAmount shouldBe BigDecimal(1253.23)
      statePension.typeDescription shouldBe "State Pension"

      val Some(benefits) = paye.benefits.get(employments.head.employmentId.toString)
      benefits.size shouldBe 2
      benefits.head.iabdType shouldBe "CarFuelBenefit"
      benefits.head.amount shouldBe BigDecimal(100)
      benefits.last.iabdType shouldBe "VanBenefit"
      benefits.last.amount shouldBe BigDecimal(100)

    }

    "successfully merge rti and nps employment1 data into employment1 list" in {

      when(testEmploymentHistoryService.npsConnector.getTaxAccount(any(), any()))
        .thenReturn(Future.successful(testNpsTaxAccount))

      val npsEmployments = npsEmploymentResponse

      val payAsYouEarn =
        testEmploymentHistoryService.mergeEmployments(
          nino = testNino,
          taxYear = TaxYear.current.previous,
          npsEmployments = npsEmployments,
          rtiEmployments = testRtiData.employments,
          taxAccountOption = Some(testNpsTaxAccount),
          iabds = testIabds
        )

      val employment = payAsYouEarn.employments.head
      val Some(payAndTax) = payAsYouEarn.payAndTax.get(employment.employmentId.toString)
      val Some(benefits) = payAsYouEarn.benefits.get(employment.employmentId.toString)
      val Some(statePension) = payAsYouEarn.statePension

      employment.employerName shouldBe "Aldi"
      employment.payeReference shouldBe "531/J4816"
      employment.startDate shouldBe startDate
      employment.endDate shouldBe None
      employment.receivingOccupationalPension shouldBe true
      payAndTax.taxablePayTotal shouldBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.taxTotal shouldBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.earlierYearUpdates.size shouldBe 1
      val eyu = payAndTax.earlierYearUpdates.head
      eyu.taxablePayEYU shouldBe BigDecimal(-600.99)
      eyu.taxEYU shouldBe BigDecimal(-10.99)
      eyu.receivedDate shouldBe new LocalDate("2016-06-01")
      benefits.size shouldBe 2
      benefits.head.iabdType shouldBe "CarFuelBenefit"
      benefits.head.amount shouldBe BigDecimal(100)
      benefits.last.iabdType shouldBe "VanBenefit"
      benefits.last.amount shouldBe BigDecimal(100)
      statePension.grossAmount shouldBe BigDecimal(1253.23)
      statePension.typeDescription shouldBe "State Pension"
    }

    "successfully exclude nps employment1 data" when {

      "nps receivingJobseekersAllowance is true for CY" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(any(), any()))
          .thenReturn(Future.successful(npsEmploymentWithJobSeekerAllowanceCY))
        when(testEmploymentHistoryService.npsConnector.getIabds(any(), any()))
          .thenReturn(Future.successful(testIabds))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(any(), any()))
          .thenReturn(Future.successful(testRtiData))

        intercept[NotFoundException](await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(TaxYear.current.currentYear))))
      }

      "otherIncomeSourceIndicator is true from list of employments" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(any(), any()))
          .thenReturn(Future.successful(npsEmploymentWithOtherIncomeSourceIndicator))
        when(testEmploymentHistoryService.npsConnector.getIabds(any(), any()))
          .thenReturn(Future.successful(testIabds))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(any(), any()))
          .thenReturn(Future.successful(testRtiData))

        intercept[NotFoundException](await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(2016))))
      }
    }

    "throw not found error" when {

      "nps employments contain single element with npsEmploymentWithJustOtherIncomeSourceIndicator attribute is true" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(any(), any()))
          .thenReturn(Future.successful(npsEmploymentWithJustOtherIncomeSourceIndicator))
        when(testEmploymentHistoryService.npsConnector.getIabds(any(), any()))
          .thenReturn(Future.successful(testIabds))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(any(), any()))
          .thenReturn(Future.successful(testRtiData))

        intercept[NotFoundException](await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(2016))))
      }
    }

    "return an empty list from get Nps Iabds api for bad request response " in {
      when(testEmploymentHistoryService.npsConnector.getIabds(any(), any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      await(testEmploymentHistoryService.retrieveNpsIabds(testNino, TaxYear(2016))) shouldBe List.empty
    }

    "return none where tax year is not cy-1" in {
      await(testEmploymentHistoryService.getTaxAccount(testNino, TaxYear(2015))) shouldBe None
    }

    "return None from get Nps Tax Account api for bad request response " in {
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(any(), any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      await(testEmploymentHistoryService.retrieveNpsTaxAccount(testNino, TaxYear(2016))) shouldBe None
    }

    "return None from get Nps Tax Account api for not found response " in {
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(any(), any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      await(testEmploymentHistoryService.retrieveNpsTaxAccount(testNino, TaxYear(2016))) shouldBe None
    }


    "get onlyRtiEmployments from List of Rti employments and List Nps Employments" in {
      val rtiEmployment1 = RtiEmployment(1, "offNo1", "ref1", None, Nil, Nil)
      val rtiEmployment2 = RtiEmployment(5, "offNo5", "ref5", None, Nil, Nil)
      val rtiEmployment3 = RtiEmployment(3, "offNo3", "ref3", None, Nil, Nil)
      val rtiEmployment4 = RtiEmployment(4, "offNo4", "ref4", None, Nil, Nil)

      val rtiEmployments = List(rtiEmployment1, rtiEmployment2, rtiEmployment3, rtiEmployment4)

      val npsEmployment1 = NpsEmployment(randomNino.toString(), 1, "offNo1", "ref1", "empname1", None,
        receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = false, LocalDate.now(), None,
        receivingOccupationalPension = false, EmploymentStatus.Live)
      val npsEmployment2 = NpsEmployment(randomNino.toString(), 2, "offNo2", "ref2", "empname2", None,
        receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = false, LocalDate.now(), None,
        receivingOccupationalPension = false, EmploymentStatus.Live)
      val npsEmployment3 = NpsEmployment(randomNino.toString(), 3, "offNo3", "ref3", "empname3", None,
        receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = false, LocalDate.now(), None,
        receivingOccupationalPension = false, EmploymentStatus.Live)

      val npsEmployments = List(npsEmployment1, npsEmployment2, npsEmployment3)

      val onlyInRti = EmploymentMatchingHelper.unmatchedRtiEmployments(npsEmployments, rtiEmployments)

      onlyInRti.nonEmpty shouldBe true
    }

    "get onlyRtiEmployments must be size 0 when all the Rti employments are matched to the Nps Employments" in {
      val rtiEmployment1 = RtiEmployment(1, "offNo1", "ref1", None, Nil, Nil)
      val rtiEmployment2 = RtiEmployment(2, "offNo2", "ref2", None, Nil, Nil)
      val rtiEmployment3 = RtiEmployment(3, "offNo3", "ref3", None, Nil, Nil)

      val rtiEmployments = List(rtiEmployment1, rtiEmployment2, rtiEmployment3)

      val npsEmployment1 = NpsEmployment(randomNino.toString(), 1, "offNo1", "ref1", "empname1", None,
        receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = false, LocalDate.now(), None,
        receivingOccupationalPension = false, EmploymentStatus.Live)
      val npsEmployment2 = NpsEmployment(randomNino.toString(), 2, "offNo2", "ref2", "empname2", None,
        receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = false, LocalDate.now(), None,
        receivingOccupationalPension = false, EmploymentStatus.Live)
      val npsEmployment3 = NpsEmployment(randomNino.toString(), 3, "offNo3", "ref3", "empname3", None,
        receivingJobSeekersAllowance = false, otherIncomeSourceIndicator = false, LocalDate.now(), None,
        receivingOccupationalPension = false, EmploymentStatus.Live)

      val npsEmployments = List(npsEmployment1, npsEmployment2, npsEmployment3)

      val onlyInRti = EmploymentMatchingHelper.unmatchedRtiEmployments(npsEmployments, rtiEmployments)

      onlyInRti shouldBe empty
    }

    "fetch Employments successfully from cache" in {
      val taxYear = TaxYear.current.previous

      val placeHolders = Seq(PlaceHolder("%taxYearStartYear%", taxYear.startYear.toString), PlaceHolder("%taxYearFinishYear%", taxYear.finishYear.toString))
      lazy val paye = loadFile("/json/withPlaceholders/model/api/paye.json", placeHolders).as[PayAsYouEarn]

      val testEmployment2 =
        Employment(UUID.fromString("01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
          locaDateCyMinus1("01", "21"), Some(locaDateCyMinus1("02", "21")), "paye-1", "employer-1",
          Some(s"/${taxYear.startYear}/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/company-benefits"),
          Some(s"/${taxYear.startYear}/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/pay-and-tax"),
          Some(s"/${taxYear.startYear}/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
          receivingOccupationalPension = false, receivingJobSeekersAllowance = false, Live, "00191048716")

      val testEmployment3 = Employment(UUID.fromString("019f5fee-d5e4-4f3e-9569-139b8ad81a87"),
        locaDateCyMinus1("02", "22"), None, "paye-2", "employer-2",
        Some(s"/${taxYear.startYear}/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87/company-benefits"),
        Some(s"/${taxYear.startYear}/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87/pay-and-tax"),
        Some(s"/${taxYear.startYear}/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87"),
        receivingOccupationalPension = false, receivingJobSeekersAllowance = false, Live, "00191048716")

      // Set up the test data in the cache
      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), taxYear), paye))

      val employments = await(testEmploymentHistoryService.getEmployments(Nino("AA000000A"), taxYear))
      employments.head.employmentStatus shouldBe EmploymentStatus.Unknown

      employments.head.startDate shouldBe taxYear.starts.withMonthOfYear(4).withDayOfMonth(6)
      employments.head.endDate shouldBe Some(taxYear.finishes.withMonthOfYear(1).withDayOfMonth(20))
      employments should contain(testEmployment2)
      employments should contain(testEmployment3)
    }

    "return an empty list when no employment(not including pensions) was returned from cache" in {
      val paye = PayAsYouEarn(employments = Nil)

      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye))

      val employments = await(testEmploymentHistoryService.getEmployments(Nino("AA000000A"), TaxYear(2014)))
      employments shouldBe List.empty
    }


    "get Employment successfully" in {
      val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      val testEmployment = Employment(UUID.fromString("01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
        new LocalDate("2016-01-21"), Some(new LocalDate("2017-01-01")), "paye-1", "employer-1",
        Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/company-benefits"),
        Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/pay-and-tax"),
        Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
        receivingOccupationalPension = false, receivingJobSeekersAllowance = false, Live, "00191048716")

      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye))

      val employment = await(testEmploymentHistoryService.getEmployment(Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))
      employment shouldBe testEmployment
    }


    "get Employment return none" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye))

      intercept[NotFoundException](await(testEmploymentHistoryService.getEmployment(
        Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae6")))
    }

    "get company benefits from cache successfully" in {
      val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      val testCompanyBenefits: List[CompanyBenefit] = List(CompanyBenefit(
        UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"), "companyBenefitType", 12))

      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye))

      val companyBenefits = await(testEmploymentHistoryService.getCompanyBenefits(
        Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))

      companyBenefits shouldBe testCompanyBenefits
    }

    "return not found when no company benefits returned from cache" in {
      lazy val payeNoBenefits = loadFile("/json/model/api/payeNoCompanyBenefits.json").as[PayAsYouEarn]

      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), payeNoBenefits))

      intercept[NotFoundException](await(testEmploymentHistoryService.getCompanyBenefits(
        Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3")))
    }

    "return no company benefits from cache for current year" in {
      val taxAccount = await(testEmploymentHistoryService.getCompanyBenefits(
        Nino("AA000000A"), TaxYear.current, "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))
      taxAccount shouldBe List.empty
    }
  }

  "withEmploymentGaps" should {

    def isNoRecordEmployment(employment: Employment): Boolean =
      employment.employerName == "No record held" && employment.employmentStatus == EmploymentStatus.Unknown

    "return the original list when no employment gaps exist" in {
      val employments = List(liveOngoingEmployment)
      testEmploymentHistoryService.addFillers(employments, TaxYear.current) shouldBe employments
    }

    "return a list with one entry when no employments exist" in {
      val employments = List.empty[Employment]
      testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(true)
    }

    "return a list with no gaps, when original employment has a gap at the start" in {
      val employments = List(liveMidYearEmployment, liveEndYearEmployment)
      testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(true, false, false)
    }

    "return a list with no gaps, when employments overlap and have gaps at the start" in {
      val employments = List(liveNoEndEmployment, liveMidYearEmployment)
      testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(true, false, false)    }

    "return a list with no gaps, when ceased employments overlap and have gaps" in {
      val employments = List(ceasedBeforeStartEmployment, ceasedNoEndEmployment, ceasedAfterEndEmployment)
      testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(false, true, false, false)
    }

    "return a list with no gaps, when potentially ceased employments overlap and have gaps" in {
      val employments = List(ceasedBeforeStartEmployment, liveMidYearEmployment, potentiallyCeasedEmployment)
      testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(false, true, false, false)
    }

    "return a list with no gaps, when original employment has a gap in the middle" in {
      val employments = List(liveStartYearEmployment, liveEndYearEmployment)
      testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(false, true, false)
    }

    "return a list with no gaps, when original employment has a gap at the end" in {
      val employments = List(liveStartYearEmployment, liveMidYearEmployment)
      testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(false, true, false, true)
    }

    "return a list with no gaps, when original employment has a gap at the start and end" in {
      val employments = List(liveMidYearEmployment)
      testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(true, false, true)
    }
  }
}