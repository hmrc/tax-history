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
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.api.{CompanyBenefit, Employment, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.nps.{EmploymentStatus, Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentMatchingHelper
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class EmploymentServiceSpec extends PlaySpec with MockitoSugar with TestUtil{

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()
  
  val testEmploymentHistoryService = TestEmploymentHistoryService.createNew()

  val npsEmploymentResponse:List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), false, false,
      new LocalDate("2015-01-21"), None, true, Live))

  val npsEmploymentWithJobSeekerAllowance:List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), true, false,
      new LocalDate("2015-01-21"), None, false, Live),
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), false, false,
      new LocalDate("2015-01-21"), None, false, Live))


  val npsEmploymentWithOtherIncomeSourceIndicator :List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), true, false,
      new LocalDate("2015-01-21"), None, false, Live),
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), false, true,
      new LocalDate("2015-01-21"), None, false, Live))


  val npsEmploymentWithJustJobSeekerAllowance :List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), true, false,
      new LocalDate("2015-01-21"), None, false, Live))

  val npsEmploymentWithJustOtherIncomeSourceIndicator :List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), false, true,
      new LocalDate("2015-01-21"), None, false, Live))


  lazy val testRtiData = loadFile("/json/rti/response/dummyRti.json").as[RtiData]
  lazy val rtiDuplicateEmploymentsResponse = loadFile("/json/rti/response/dummyRtiDuplicateEmployments.json")
  lazy val rtiPartialDuplicateEmploymentsResponse = loadFile("/json/rti/response/dummyRtiPartialDuplicateEmployments.json")
  lazy val rtiNonMatchingEmploymentsResponse = loadFile("/json/rti/response/dummyRtiNonMatchingEmployment.json")
  lazy val testNpsTaxAccount = loadFile("/json/nps/response/GetTaxAccount.json").as[NpsTaxAccount]
  lazy val testIabds = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]

  val startDate = new LocalDate("2015-01-21")

  "Employment Service" should {
    "successfully get Nps Employments Data" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(npsEmploymentResponse))

      noException shouldBe thrownBy(await(testEmploymentHistoryService.retrieveNpsEmployments(testNino, TaxYear(2016))))
    }

    "return any non success status response from get Nps Employments api" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      intercept[BadRequestException](await(testEmploymentHistoryService.retrieveNpsEmployments(testNino, TaxYear(2016))))
    }

    "successfully get Rti Employments Data" in {
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(testRtiData))

      val result = await(testEmploymentHistoryService.retrieveRtiData(testNino, TaxYear(2016)))
      result mustBe testRtiData
    }

    "return not found status response from get Nps Employments api" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Nil))
      intercept[NotFoundException](await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino,TaxYear(2016))))
    }

    "return success status despite failing response from get Rti Employments api when there are nps employments" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      noException shouldBe thrownBy {
        await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino,TaxYear(2016)))
      }
    }

    "return success response from get Employments" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(testIabds))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(testRtiData))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      val paye = await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino,TaxYear(2016)))

      val employments = paye.employments
      employments.size mustBe 1
      employments.head.employerName mustBe "Aldi"
      employments.head.payeReference mustBe "531/J4816"
      employments.head.startDate mustBe startDate
      employments.head.endDate mustBe None

      val Some(payAndTax) = paye.payAndTax.get(employments.head.employmentId.toString)
      payAndTax.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.earlierYearUpdates.size mustBe 1

      val eyu = payAndTax.earlierYearUpdates.head
      eyu.receivedDate mustBe new LocalDate("2016-06-01")
      eyu.taxablePayEYU mustBe BigDecimal(-600.99)
      eyu.taxEYU mustBe BigDecimal(-10.99)

      val Some(benefits) = paye.benefits.get(employments.head.employmentId.toString)
      benefits.size mustBe 2
      benefits.head.iabdType mustBe "CarFuelBenefit"
      benefits.head.amount mustBe BigDecimal(100)
      benefits.last.iabdType mustBe "VanBenefit"
      benefits.last.amount mustBe BigDecimal(100)

    }

    "successfully merge rti and nps employment1 data into employment1 list" in {

      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any()))
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

      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "531/J4816"
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      employment.receivingOccupationalPension mustBe true
      payAndTax.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.earlierYearUpdates.size mustBe 1
      val eyu = payAndTax.earlierYearUpdates.head
      eyu.taxablePayEYU mustBe BigDecimal(-600.99)
      eyu.taxEYU mustBe BigDecimal(-10.99)
      eyu.receivedDate mustBe new LocalDate("2016-06-01")
      benefits.size mustBe 2
      benefits.head.iabdType mustBe "CarFuelBenefit"
      benefits.head.amount mustBe BigDecimal(100)
      benefits.last.iabdType mustBe "VanBenefit"
      benefits.last.amount mustBe BigDecimal(100)

    }


    "successfully exclude nps employment1 data" when {
      "nps receivingJobseekersAllowance is true from list of employments" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(npsEmploymentWithJobSeekerAllowance))
        when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(testIabds))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(testRtiData))
        val payAsYouEarn = await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino,TaxYear(2016)))

        val employments = payAsYouEarn.employments
        employments.size mustBe 1
      }

      "nps receivingJobseekersAllowance and otherIncomeSourceIndicator is true from list of employments" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(npsEmploymentWithOtherIncomeSourceIndicator))
        when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(testIabds))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(testRtiData))

        intercept[NotFoundException](await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino,TaxYear(2016))))
      }
    }

    "throw not found error" when {
      "nps employments contain single element with receivingJobseekersAllowance attribute is true" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(npsEmploymentWithJustJobSeekerAllowance))
        when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(testIabds))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(testRtiData))

        intercept[NotFoundException](await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino,TaxYear(2016))))
      }

      "nps employments contain single element with npsEmploymentWithJustOtherIncomeSourceIndicator attribute is true" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(npsEmploymentWithJustOtherIncomeSourceIndicator))
        when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(testIabds))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(testRtiData))

        intercept[NotFoundException](await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino,TaxYear(2016))))
      }
    }

    "return any non success status response from get Nps Iabds api" in {
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      intercept[BadRequestException](await(testEmploymentHistoryService.retrieveNpsIabds(testNino,TaxYear(2016))))
    }

    "return none where tax year is not cy-1" in {
      val response = await(testEmploymentHistoryService.getNpsTaxAccount(testNino, TaxYear(2015)))
      response mustBe None
    }

    "return any non success status response from get Nps Tax Account api" in {
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      intercept[BadRequestException](await(testEmploymentHistoryService.getNpsTaxAccount(testNino,TaxYear(2016))))
    }

    "return not found status response from get Nps Tax Account api" in {
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      intercept[NotFoundException](await(testEmploymentHistoryService.getNpsTaxAccount(testNino,TaxYear(2016))))
    }


    "get onlyRtiEmployments  from List of Rti employments and List Nps Employments" in {
      val rtiEmployment1 = RtiEmployment(1,"offNo1","ref1",None,Nil,Nil)
      val rtiEmployment2 = RtiEmployment(5,"offNo5","ref5",None,Nil,Nil)
      val rtiEmployment3 = RtiEmployment(3,"offNo3","ref3",None,Nil,Nil)
      val rtiEmployment4 = RtiEmployment(4,"offNo4","ref4",None,Nil,Nil)

      val rtiEmployments = List(rtiEmployment1,rtiEmployment2,rtiEmployment3,rtiEmployment4)

      val npsEmployment1 = NpsEmployment(randomNino.toString(),1,"offNo1","ref1","empname1",None,false,false,LocalDate.now(),None, false, EmploymentStatus.Live)
      val npsEmployment2 = NpsEmployment(randomNino.toString(),2,"offNo2","ref2","empname2",None,false,false,LocalDate.now(),None, false, EmploymentStatus.Live)
      val npsEmployment3 = NpsEmployment(randomNino.toString(),3,"offNo3","ref3","empname3",None,false,false,LocalDate.now(),None, false, EmploymentStatus.Live)
      val npsEmployments = List(npsEmployment1,npsEmployment2,npsEmployment3)
      val rtiData = RtiData("QQ0000002", rtiEmployments)
      val mockAuditable = mock[Auditable]

      val onlyInRti = EmploymentMatchingHelper.unmatchedRtiEmployments(npsEmployments, rtiEmployments)

      onlyInRti must not be empty
    }

    "get onlyRtiEmployments must be size 0 when all the Rti employments are matched to the Nps Employments" in {
      val rtiEmployment1 = RtiEmployment(1,"offNo1","ref1",None,Nil,Nil)
      val rtiEmployment2 = RtiEmployment(2,"offNo2","ref2",None,Nil,Nil)
      val rtiEmployment3 = RtiEmployment(3,"offNo3","ref3",None,Nil,Nil)

      val rtiEmployments = List(rtiEmployment1,rtiEmployment2,rtiEmployment3)

      val npsEmployment1 = NpsEmployment(randomNino.toString(),1,"offNo1","ref1","empname1",None,false,false,LocalDate.now(),None, false, EmploymentStatus.Live)
      val npsEmployment2 = NpsEmployment(randomNino.toString(),2,"offNo2","ref2","empname2",None,false,false,LocalDate.now(),None, false, EmploymentStatus.Live)
      val npsEmployment3 = NpsEmployment(randomNino.toString(),3,"offNo3","ref3","empname3",None,false,false,LocalDate.now(),None, false, EmploymentStatus.Live)
      val npsEmployments = List(npsEmployment1,npsEmployment2,npsEmployment3)
      val rtiData = RtiData("QQ0000002", rtiEmployments)
      val mockAuditable = mock[Auditable]

      val onlyInRti = EmploymentMatchingHelper.unmatchedRtiEmployments(npsEmployments, rtiEmployments)

      onlyInRti must be (empty)
    }

    "fetch Employments successfully from cache" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]


      val testEmployments:List[Employment]=List(
        Employment(UUID.fromString("01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
          new LocalDate("2016-01-21"), Some(new LocalDate("2017-01-01")), "paye-1", "employer-1",
          Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/company-benefits"),
          Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/pay-and-tax"),
          Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3"), false, Live),

        Employment(UUID.fromString("019f5fee-d5e4-4f3e-9569-139b8ad81a87"),
          new LocalDate("2016-02-22"), None, "paye-2", "employer-2",
          Some("/2014/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87/company-benefits"),
          Some("/2014/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87/pay-and-tax"),
          Some("/2014/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87"), false, Live
        )
      )

      // Set up the test data in the cache
      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye))

      val employments = await(testEmploymentHistoryService.getEmployments(Nino("AA000000A"), TaxYear(2014)))
      employments must be (testEmployments)
    }

    "return not found when no employment was returned from cache" in {
      val paye = PayAsYouEarn(employments = Nil)

      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye))

      intercept[NotFoundException](await(testEmploymentHistoryService.getEmployments(Nino("AA000000A"), TaxYear(2014))))
    }


    "get Employment successfully" in {
      val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      val testEmployment = Employment(UUID.fromString("01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
        new LocalDate("2016-01-21"), Some(new LocalDate("2017-01-01")), "paye-1", "employer-1",
        Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/company-benefits"),
        Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/pay-and-tax"),
        Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3"), false, Live
      )

      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye))

      val employment = await(testEmploymentHistoryService.getEmployment(Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))
      employment must be (testEmployment)
    }


    "get Employment return none" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye))

      intercept[NotFoundException](await(testEmploymentHistoryService.getEmployment(Nino("AA000000A"), TaxYear(2014),"01318d7c-bcd9-47e2-8c38-551e7ccdfae6")))
    }

    "get company benefits from cache successfully" in {
      val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      val testCompanyBenefits:List[CompanyBenefit] = List(CompanyBenefit(UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),"companyBenefitType", 12))

      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), paye))

      val companyBenefits = await(testEmploymentHistoryService.getCompanyBenefits(Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))

      companyBenefits must be (testCompanyBenefits)
    }

    "return not found when no company benefits returned from cache" in {
      lazy val payeNoBenefits = loadFile("/json/model/api/payeNoCompanyBenefits.json").as[PayAsYouEarn]

      await(testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), TaxYear(2014)), payeNoBenefits))

      intercept[NotFoundException](await(testEmploymentHistoryService.getCompanyBenefits(Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3")))
    }
  }
}