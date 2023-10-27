/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, NotFoundException}
import uk.gov.hmrc.taxhistory.fixtures.Employments
import uk.gov.hmrc.taxhistory.model.api.EmploymentPaymentType.OccupationalPension
import uk.gov.hmrc.taxhistory.model.api._
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.utils.{PlaceHolder, TestEmploymentHistoryService}
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Await.result
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class EmploymentHistoryServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with OptionValues
    with EmploymentHistoryServiceBaseSpec
    with Employments {

  "EmploymentHistoryService" when {
    ".retrieveNpsEmployments" should {
      "successfully get Nps Employments Data" in
        new StubConnectors(npsGetEmployments = stubNpsGetEmploymentsSucceeds(npsEmploymentResponse)) {
          noException shouldBe thrownBy(
            testEmploymentHistoryService.retrieveNpsEmployments(testNino, TaxYear(taxYear)).futureValue
          )
        }

      "successfully get Nps Employments Data with jobseekers allowance for cy-1" in
        new StubConnectors(
          npsGetEmployments = stubNpsGetEmploymentsSucceeds(npsEmploymentWithJobSeekerAllowanceCYMinus1)
        ) {
          noException shouldBe thrownBy(
            testEmploymentHistoryService.retrieveNpsEmployments(testNino, TaxYear(taxYear)).futureValue
          )
        }

      "return any non success status response from get Nps Employments api" in
        new StubConnectors(npsGetEmployments = stubNpsGetEmploymentsFails(new BadRequestException(""))) {
          intercept[BadRequestException](
            await(testEmploymentHistoryService.retrieveNpsEmployments(testNino, TaxYear(taxYear)))
          )
        }

      "successfully get Rti Employments Data" in
        new StubConnectors(rti = stubRtiGetEmploymentsSucceeds(Some(testRtiData))) {
          testEmploymentHistoryService.retrieveRtiData(testNino, TaxYear(taxYear)).futureValue shouldBe Some(
            testRtiData
          )
        }

      "successfully get no RTI employments data if RTI connector returns None" in
        new StubConnectors(rti = stubRtiGetEmploymentsSucceeds(None)) {
          testEmploymentHistoryService.retrieveRtiData(testNino, TaxYear(taxYear)).futureValue shouldBe None
        }

      "fail with NotFoundException if the NPS Get Employments API was successful but returned zero employments" in
        new StubConnectors(npsGetEmployments = stubNpsGetEmploymentsSucceeds(List.empty)) {
          intercept[NotFoundException](
            await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(taxYear)))
          )
        }

      "fail with NotFoundException if the NPS Get Employments API failed with a NotFoundException" in
        new StubConnectors(
          npsGetEmployments = stubNpsGetEmploymentsFails(new NotFoundException("NPS API returned 404"))
        ) {
          intercept[NotFoundException](
            await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(taxYear)))
          )
        }

      "throw an exception when the call to get RTI employments fails" in
        new StubConnectors(rti = stubRtiGetEmploymentsFails(new BadRequestException(""))) {
          intercept[BadRequestException](
            await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(taxYear)))
          )
        }

      "succeeds when the RTI call fails with a 404 (i.e the RTI connector returns None)" in
        new StubConnectors(rti = stubRtiGetEmploymentsSucceeds(None)) {
          noException shouldBe thrownBy(
            testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(taxYear)).futureValue
          )
        }

      "succeeds when the get IABD call fails with a 404 (i.e the RTI connector returns None)" in
        new StubConnectors(npsGetIabdDetails = stubNpsGetIabdsSucceeds(List())) {
          noException shouldBe thrownBy(
            testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(taxYear)).futureValue
          )
        }

      "throw an exception when the call to get NPS tax account fails" in
        new StubConnectors(npsGetTaxAccount = stubNpsGetTaxAccountFails(new BadRequestException(""))) {
          intercept[BadRequestException](
            await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(taxYear)))
          )
        }

      "return success response from get Employments" in {
        stubNpsGetEmploymentsSucceeds(npsEmploymentResponse)
        stubNpsGetIabdsSucceeds(testIabds)
        stubRtiGetEmploymentsSucceeds(Some(testRtiData))
        stubNpsGetTaxAccountSucceeds(Some(testNpsTaxAccount))

        val paye = testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(taxYear)).futureValue

        val amount      = 100
        val employments = paye.employments
        employments.size                       shouldBe 1
        employments.head.employerName          shouldBe "Aldi"
        employments.head.payeReference         shouldBe "531/J4816"
        employments.head.startDate             shouldBe Some(startDate)
        employments.head.endDate               shouldBe None
        employments.head.employmentPaymentType shouldBe Some(OccupationalPension)

        val payAndTax = paye.payAndTax.get(employments.head.employmentId.toString).value
        payAndTax.taxablePayTotal             shouldBe Some(BigDecimal.valueOf(20000.00))
        payAndTax.taxablePayTotalIncludingEYU shouldBe Some(BigDecimal.valueOf(19399.01))
        payAndTax.taxTotal                    shouldBe Some(BigDecimal.valueOf(1880.00))
        payAndTax.taxTotalIncludingEYU        shouldBe Some(BigDecimal.valueOf(1869.01))
        payAndTax.earlierYearUpdates.size     shouldBe 1

        val eyu = payAndTax.earlierYearUpdates.head
        eyu.receivedDate  shouldBe LocalDate.of(YEAR_2016, JUNE, DAY_1)
        eyu.taxablePayEYU shouldBe BigDecimal(-600.99)
        eyu.taxEYU        shouldBe BigDecimal(-10.99)

        val statePension = paye.statePension.value
        statePension.grossAmount     shouldBe BigDecimal(1253.23)
        statePension.typeDescription shouldBe "State Pension"

        val benefits = paye.benefits.get(employments.head.employmentId.toString).value
        benefits.size          shouldBe 2
        benefits.head.iabdType shouldBe "CarFuelBenefit"
        benefits.head.amount   shouldBe BigDecimal(amount)
        benefits.last.iabdType shouldBe "VanBenefit"
        benefits.last.amount   shouldBe BigDecimal(amount)
      }

      "successfully merge rti and nps employment1 data into employment1 list" in {
        val payAsYouEarn =
          testEmploymentHistoryService.mergeEmployments(
            nino = testNino,
            taxYear = TaxYear.current.previous,
            npsEmployments = npsEmploymentResponse,
            rtiEmployments = testRtiData.employments,
            taxAccountOption = Some(testNpsTaxAccount),
            iabds = testIabds
          )

        val amount       = 100
        val employment   = payAsYouEarn.employments.head
        val payAndTax    = payAsYouEarn.payAndTax.get(employment.employmentId.toString).value
        val benefits     = payAsYouEarn.benefits.get(employment.employmentId.toString).value
        val statePension = payAsYouEarn.statePension.value

        employment.employerName               shouldBe "Aldi"
        employment.payeReference              shouldBe "531/J4816"
        employment.startDate                  shouldBe Some(startDate)
        employment.endDate                    shouldBe None
        employment.isOccupationalPension      shouldBe true
        employment.employmentPaymentType      shouldBe Some(OccupationalPension)
        payAndTax.taxablePayTotal             shouldBe Some(BigDecimal.valueOf(20000.00))
        payAndTax.taxablePayTotalIncludingEYU shouldBe Some(BigDecimal.valueOf(19399.01))
        payAndTax.taxTotal                    shouldBe Some(BigDecimal.valueOf(1880.00))
        payAndTax.taxTotalIncludingEYU        shouldBe Some(BigDecimal.valueOf(1869.01))
        payAndTax.earlierYearUpdates.size     shouldBe 1
        val eyu = payAndTax.earlierYearUpdates.head
        eyu.taxablePayEYU            shouldBe BigDecimal(-600.99)
        eyu.taxEYU                   shouldBe BigDecimal(-10.99)
        eyu.receivedDate             shouldBe LocalDate.of(YEAR_2016, JUNE, DAY_1)
        benefits.size                shouldBe 2
        benefits.head.iabdType       shouldBe "CarFuelBenefit"
        benefits.head.amount         shouldBe BigDecimal(amount)
        benefits.last.iabdType       shouldBe "VanBenefit"
        benefits.last.amount         shouldBe BigDecimal(amount)
        statePension.grossAmount     shouldBe BigDecimal(1253.23)
        statePension.typeDescription shouldBe "State Pension"
      }

      "successfully exclude nps employment1 data" when {

        "nps receivingJobseekersAllowance is true for CY" in
          new StubConnectors(npsGetEmployments = stubNpsGetEmploymentsSucceeds(npsEmploymentWithJobSeekerAllowanceCY)) {
            intercept[NotFoundException](
              await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(TaxYear.current.currentYear)))
            )
          }

        "otherIncomeSourceIndicator is true from list of employments" in
          new StubConnectors(
            npsGetEmployments = stubNpsGetEmploymentsSucceeds(npsEmploymentWithOtherIncomeSourceIndicator)
          ) {
            intercept[NotFoundException](
              await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(taxYear)))
            )
          }
      }

      "throw not found error" when {

        "nps employments contain single element with npsEmploymentWithJustOtherIncomeSourceIndicator attribute is true" in
          new StubConnectors(
            npsGetEmployments = stubNpsGetEmploymentsSucceeds(npsEmploymentWithJustOtherIncomeSourceIndicator)
          ) {
            intercept[NotFoundException](
              await(testEmploymentHistoryService.retrieveAndBuildPaye(testNino, TaxYear(taxYear)))
            )
          }
      }

      "propagate exception if NPS IABD API connector fails with an exception" in
        new StubConnectors(npsGetIabdDetails = stubNpsGetIabdFails(new BadRequestException(""))) {
          intercept[BadRequestException](
            await(testEmploymentHistoryService.retrieveNpsIabds(testNino, TaxYear(taxYear)))
          )
        }

      "propagate exception if NPS Get Tax Account API connector fails with an exception" in
        new StubConnectors(npsGetTaxAccount = stubNpsGetTaxAccountFails(new BadRequestException(""))) {
          intercept[BadRequestException](
            await(testEmploymentHistoryService.getTaxAccount(testNino, TaxYear.current.previous))
          )
        }

      "fail with a NotFoundException if NPS Get Tax Account API connector returns None (i.e. the API returned a 404)" when {
        "asking for current year" in
          new StubConnectors(npsGetTaxAccount = stubNpsGetTaxAccountSucceeds(None)) {
            intercept[NotFoundException](await(testEmploymentHistoryService.getTaxAccount(testNino, TaxYear.current)))
          }

        "asking for previous year" in
          new StubConnectors(npsGetTaxAccount = stubNpsGetTaxAccountSucceeds(None)) {
            intercept[NotFoundException](
              await(testEmploymentHistoryService.getTaxAccount(testNino, TaxYear.current.previous))
            )
          }
      }

      "fail with a NotFoundException  from get Nps Tax Account api for not found response " in {
        when(testEmploymentHistoryService.desNpsConnector.getTaxAccount(any(), any()))
          .thenReturn(Future.failed(new NotFoundException("")))
        intercept[NotFoundException](
          await(testEmploymentHistoryService.retrieveNpsTaxAccount(testNino, TaxYear(taxYear)))
        )
      }

      "fetch Employments successfully from cache" in {
        val taxYear = TaxYear.current.previous

        val placeHolders = Seq(
          PlaceHolder("%taxYearStartYear%", taxYear.startYear.toString),
          PlaceHolder("%taxYearFinishYear%", taxYear.finishYear.toString)
        )
        lazy val paye    = loadFile("/json/withPlaceholders/model/api/paye.json", placeHolders).as[PayAsYouEarn]

        // scalastyle:off magic.number
        val testEmployment2 =
          Employment(
            UUID.fromString("01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
            Some(locaDateCyMinus1(1, 21)),
            Some(locaDateCyMinus1(2, 21)),
            "paye-1",
            "employer-1",
            Some(s"/${taxYear.startYear}/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/company-benefits"),
            Some(s"/${taxYear.startYear}/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/pay-and-tax"),
            Some(s"/${taxYear.startYear}/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
            None,
            Live,
            "00191048716"
          )

        val testEmployment3 = Employment(
          UUID.fromString("019f5fee-d5e4-4f3e-9569-139b8ad81a87"),
          Some(locaDateCyMinus1(2, 22)),
          None,
          "paye-2",
          "employer-2",
          Some(s"/${taxYear.startYear}/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87/company-benefits"),
          Some(s"/${taxYear.startYear}/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87/pay-and-tax"),
          Some(s"/${taxYear.startYear}/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87"),
          None,
          Live,
          "00191048716"
        )

        // Set up the test data in the cache
        testEmploymentHistoryService.cacheService.insertOrUpdate((Nino("AA000000A"), taxYear), paye).futureValue
        val april        = 4
        val january      = 1
        val dayOfMonth6  = 6
        val dayOfMonth20 = 20

        val employments = testEmploymentHistoryService.getEmployments(Nino("AA000000A"), taxYear).futureValue
        employments.head.employmentStatus shouldBe EmploymentStatus.Unknown

        employments.head.startDate shouldBe Some(taxYear.starts.withMonth(april).withDayOfMonth(dayOfMonth6))
        employments.head.endDate   shouldBe Some(taxYear.finishes.withMonth(january).withDayOfMonth(dayOfMonth20))
        employments                  should contain(testEmployment2)
        employments                  should contain(testEmployment3)
      }

      "return an empty list when no employment(not including pensions) was returned from cache" in {
        val paye = PayAsYouEarn(employments = Nil)

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((Nino("AA000000A"), TaxYear(taxYear2)), paye)
          .futureValue

        val employments = testEmploymentHistoryService.getEmployments(Nino("AA000000A"), TaxYear(taxYear2)).futureValue
        employments shouldBe List.empty
      }

      "get Employment successfully" in {
        val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

        val testEmployment = Employment(
          UUID.fromString("01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
          Some(LocalDate.of(YEAR_2016, JANUARY, DAY_21)),
          Some(LocalDate.of(YEAR_2017, JANUARY, DAY_1)),
          "paye-1",
          "employer-1",
          Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/company-benefits"),
          Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/pay-and-tax"),
          Some("/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3"),
          None,
          Live,
          "00191048716"
        )

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((Nino("AA000000A"), TaxYear(taxYear2)), paye)
          .futureValue

        val employment = testEmploymentHistoryService
          .getEmployment(Nino("AA000000A"), TaxYear(taxYear2), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3")
          .futureValue
        employment shouldBe testEmployment
      }

      "get Employment return none" in {
        lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((Nino("AA000000A"), TaxYear(taxYear2)), paye)
          .futureValue

        intercept[NotFoundException](
          await(
            testEmploymentHistoryService
              .getEmployment(Nino("AA000000A"), TaxYear(taxYear2), "01318d7c-bcd9-47e2-8c38-551e7ccdfae6")
          )
        )
      }

      "get company benefits from cache successfully" in {
        val paye   = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]
        val amount = 12

        val testCompanyBenefits: List[CompanyBenefit] =
          List(CompanyBenefit(UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"), "companyBenefitType", amount))

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((Nino("AA000000A"), TaxYear(taxYear2)), paye)
          .futureValue

        val companyBenefits = testEmploymentHistoryService
          .getCompanyBenefits(Nino("AA000000A"), TaxYear(taxYear2), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3")
          .futureValue

        companyBenefits shouldBe testCompanyBenefits
      }

      "return not found when no company benefits returned from cache" in {
        lazy val payeNoBenefits = loadFile("/json/model/api/payeNoCompanyBenefits.json").as[PayAsYouEarn]

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((Nino("AA000000A"), TaxYear(taxYear2)), payeNoBenefits)
          .futureValue

        intercept[NotFoundException](
          await(
            testEmploymentHistoryService
              .getCompanyBenefits(Nino("AA000000A"), TaxYear(taxYear2), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3")
          )
        )
      }

      "return no company benefits from cache for current year" in {
        val taxAccount = testEmploymentHistoryService
          .getCompanyBenefits(Nino("AA000000A"), TaxYear.current, "01318d7c-bcd9-47e2-8c38-551e7ccdfae3")
          .futureValue
        taxAccount shouldBe List.empty
      }
    }

    ".addFillers" should {

      def isNoRecordEmployment(employment: Employment): Boolean =
        employment.employerName == "No record held" && employment.employmentStatus == EmploymentStatus.Unknown

      "return the original list when no employment gaps exist" in {
        val employments = List(liveOngoingEmployment)
        testEmploymentHistoryService.addFillers(employments, TaxYear.current) shouldBe employments
      }

      "return a list with one entry when no employments exist" in {
        val employments = List.empty[Employment]
        testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(
          true
        )
      }

      "return a list with no gaps, when original employment has a gap at the start" in {
        val employments = List(liveMidYearEmployment, liveEndYearEmployment)
        testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(
          true,
          false,
          false
        )
      }

      "return a list with no gaps, when employments overlap and have gaps at the start" in {
        val employments = List(liveNoEndEmployment, liveMidYearEmployment)
        testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(
          true,
          false,
          false
        )
      }

      "return a list with no gaps, when ceased employments overlap and have gaps" in {
        val employments = List(ceasedBeforeStartEmployment, ceasedNoEndEmployment, ceasedAfterEndEmployment)
        testEmploymentHistoryService
          .addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(false, true, false, false)
      }

      "return a list with no gaps, when potentially ceased employments overlap and have gaps" in {
        val employments = List(ceasedBeforeStartEmployment, liveMidYearEmployment, potentiallyCeasedEmployment)
        testEmploymentHistoryService
          .addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(false, true, false, false)
      }

      "return a list with no gaps, when original employment has a gap in the middle" in {
        val employments = List(liveStartYearEmployment, liveEndYearEmployment)
        testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(
          false,
          true,
          false
        )
      }

      "return a list with no gaps, when original employment has a gap at the end" in {
        val employments = List(liveStartYearEmployment, liveMidYearEmployment)
        testEmploymentHistoryService
          .addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(false, true, false, true)
      }

      "return a list with no gaps, when original employment has a gap at the start and end" in {
        val employments = List(liveMidYearEmployment)
        testEmploymentHistoryService.addFillers(employments, TaxYear.current) map isNoRecordEmployment shouldBe Seq(
          true,
          false,
          true
        )
      }

      def oneDayBefore(thisDay: Option[LocalDate]): Option[LocalDate] = thisDay.map(_.minusDays(1))

      def oneDayAfter(thisDay: Option[LocalDate]): Option[LocalDate] = thisDay.map(_.plusDays(1))

      "when original employment has no start date but has an end date, and there are no employments afterwards" in {
        val employmentWithoutStartDate = liveMidYearEmployment.copy(startDate = None)
        val filledEmployments          =
          testEmploymentHistoryService.addFillers(List(employmentWithoutStartDate), TaxYear.current)

        val isOrderedFirstInList = filledEmployments.head == employmentWithoutStartDate
        isOrderedFirstInList shouldBe true

        filledEmployments map isNoRecordEmployment shouldBe Seq(false, true, true)

        val firstGap  = filledEmployments(1)
        val secondGap = filledEmployments(2)
        firstGap.startDate shouldBe Some(TaxYear.current.starts)
        firstGap.endDate   shouldBe oneDayBefore(employmentWithoutStartDate.endDate)

        secondGap.startDate shouldBe oneDayAfter(employmentWithoutStartDate.endDate)
        secondGap.endDate   shouldBe None
      }

      "when original employment has no start date but has an end date, and the employment finishes at the end of the tax year" in {
        val employmentWithoutStartDate = liveEndYearEmployment.copy(startDate = None)
        val filledEmployments          =
          testEmploymentHistoryService.addFillers(List(employmentWithoutStartDate), TaxYear.current)

        val isOrderedFirstInList = filledEmployments.head == employmentWithoutStartDate
        isOrderedFirstInList shouldBe true

        filledEmployments map isNoRecordEmployment shouldBe Seq(false, true)

        val gap = filledEmployments(1)
        gap.startDate shouldBe Some(TaxYear.current.starts)
        gap.endDate   shouldBe oneDayBefore(employmentWithoutStartDate.endDate)
      }

      "when original employment has no start date but has an end date, and the employment finishes on the first day of the tax year" in {
        val employmentWithoutStartDate =
          liveOngoingEmployment.copy(startDate = None, endDate = Some(TaxYear.current.starts))
        val filledEmployments          =
          testEmploymentHistoryService.addFillers(List(employmentWithoutStartDate), TaxYear.current)

        val isOrderedFirstInList = filledEmployments.head == employmentWithoutStartDate
        isOrderedFirstInList shouldBe true

        filledEmployments map isNoRecordEmployment shouldBe Seq(false, true)

        val gap = filledEmployments(1)
        gap.startDate shouldBe oneDayAfter(employmentWithoutStartDate.endDate)
        gap.endDate   shouldBe None
      }

      "when original employment has no start date but has an end date, and there is another employment immediately afterwards" in {
        val employmentWithoutStartDate = liveMidYearEmployment.copy(startDate = None)
        val subsequentEmployment       = liveMidYearEmployment.copy(
          startDate = oneDayAfter(liveMidYearEmployment.endDate),
          endDate = None
        )
        val filledEmployments          =
          testEmploymentHistoryService.addFillers(
            List(employmentWithoutStartDate, subsequentEmployment),
            TaxYear.current
          )

        val isOrderedFirstInList = filledEmployments.head == employmentWithoutStartDate
        isOrderedFirstInList shouldBe true

        filledEmployments map isNoRecordEmployment shouldBe Seq(false, true, false)

        val gap = filledEmployments(1)
        gap.startDate shouldBe Some(TaxYear.current.starts)
        gap.endDate   shouldBe oneDayBefore(employmentWithoutStartDate.endDate)
      }

      "when original employment has no start date but has an end date, and there is another employment some days later" in {
        val employmentWithoutStartDate = liveMidYearEmployment.copy(startDate = None)
        val subsequentEmployment       = liveMidYearEmployment.copy(
          startDate = liveMidYearEmployment.endDate.map(_.plusDays(3)),
          endDate = None
        )
        val filledEmployments          =
          testEmploymentHistoryService.addFillers(
            List(employmentWithoutStartDate, subsequentEmployment),
            TaxYear.current
          )

        val isOrderedFirstInList = filledEmployments.head == employmentWithoutStartDate
        isOrderedFirstInList shouldBe true

        filledEmployments map isNoRecordEmployment shouldBe Seq(false, true, true, false)

        val firstGap = filledEmployments(1)
        firstGap.startDate shouldBe Some(TaxYear.current.starts)
        firstGap.endDate   shouldBe oneDayBefore(employmentWithoutStartDate.endDate)

        val secondGap = filledEmployments(2)
        secondGap.startDate shouldBe oneDayAfter(employmentWithoutStartDate.endDate)
        secondGap.endDate   shouldBe oneDayBefore(subsequentEmployment.startDate)
      }

      "when original employment has no start date but has an end date, and there is another employment immediately before (its end date)" in {
        val preceedingEmployment       = liveMidYearEmployment.copy(
          startDate = Some(TaxYear.current.starts),
          endDate = oneDayBefore(liveMidYearEmployment.endDate)
        )
        val employmentWithoutStartDate = liveMidYearEmployment.copy(startDate = None)

        val filledEmployments =
          testEmploymentHistoryService.addFillers(
            List(preceedingEmployment, employmentWithoutStartDate),
            TaxYear.current
          )

        val isOrderedFirstInList = filledEmployments.head == employmentWithoutStartDate
        isOrderedFirstInList shouldBe true

        filledEmployments map isNoRecordEmployment shouldBe Seq(false, false, true)

        val gap = filledEmployments(2)
        gap.startDate shouldBe oneDayAfter(employmentWithoutStartDate.endDate)
        gap.endDate   shouldBe None
      }

      "when original employment has no start date but has an end date, and there is another employment some days before (its end date)" in {
        val preceedingEmployment       = liveMidYearEmployment.copy(
          startDate = Some(TaxYear.current.starts),
          endDate = liveMidYearEmployment.endDate.map(_.minusDays(3))
        )
        val employmentWithoutStartDate = liveMidYearEmployment.copy(startDate = None)

        val filledEmployments =
          testEmploymentHistoryService.addFillers(
            List(preceedingEmployment, employmentWithoutStartDate),
            TaxYear.current
          )

        val isOrderedFirstInList = filledEmployments.head == employmentWithoutStartDate
        isOrderedFirstInList shouldBe true

        filledEmployments map isNoRecordEmployment shouldBe Seq(false, false, true, true)

        val firstGap = filledEmployments(2)
        firstGap.startDate shouldBe oneDayAfter(preceedingEmployment.endDate)
        firstGap.endDate   shouldBe oneDayBefore(employmentWithoutStartDate.endDate)

        val secondGap = filledEmployments(3)
        secondGap.startDate shouldBe oneDayAfter(employmentWithoutStartDate.endDate)
        secondGap.endDate   shouldBe None
      }

      "when original employment has no start date and has no end date, and there are no other employments" in {
        val employmentNoDates = liveOngoingEmployment.copy(startDate = None, endDate = None)

        val filledEmployments = testEmploymentHistoryService.addFillers(List(employmentNoDates), TaxYear.current)

        filledEmployments shouldBe List(employmentNoDates)
      }

      "when original employment has no start date and has no end date, and there is another employment" in {
        val employmentNoDates = liveOngoingEmployment.copy(startDate = None, endDate = None)

        val filledEmployments =
          testEmploymentHistoryService.addFillers(List(employmentNoDates, liveMidYearEmployment), TaxYear.current)

        val isOrderedFirstInList = filledEmployments.head == employmentNoDates
        isOrderedFirstInList shouldBe true

        filledEmployments map isNoRecordEmployment shouldBe Seq(false, false)
      }

      "when there are two employments without start dates but with end dates" in {
        val employment1NoDates = liveOngoingEmployment.copy(startDate = None, endDate = None)
        val employment2NoDates = liveOngoingEmployment.copy(startDate = None, endDate = None)

        val filledEmployments =
          testEmploymentHistoryService.addFillers(List(employment1NoDates, employment2NoDates), TaxYear.current)

        filledEmployments map isNoRecordEmployment shouldBe Seq(false, false)
      }
    }

    ".getIncomeSource" should {

      def enableFlag(): Unit =
        when(TestEmploymentHistoryService.mockAppConfig.taxAccountPreviousYearsFlag).thenReturn(true)

      def disableFlag(): Unit =
        when(TestEmploymentHistoryService.mockAppConfig.taxAccountPreviousYearsFlag).thenReturn(false)

      "get Income Source return None when no data and taxAccountPreviousYearsFlag = true" in {
        enableFlag()

        val empRefId = "invalidEmpId"

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((testNino, TaxYear.current), payAsYouEarn)
          .futureValue

        val exception = intercept[NotFoundException] {
          result(testEmploymentHistoryService.getIncomeSource(testNino, TaxYear.current, empRefId), Duration.Inf)
        }

        exception.message shouldBe s"IncomeSource not found for NINO ${testNino.nino}, tax year ${TaxYear.current
          .toString()}, and employmentId $empRefId"
      }

      "get Income Source return Data for current year when no data and taxAccountPreviousYearsFlag = true" in {
        enableFlag()

        val empRefId = payAsYouEarn.incomeSources.keys.head

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((testNino, TaxYear.current), payAsYouEarn)
          .futureValue
        val incomeSource =
          result(testEmploymentHistoryService.getIncomeSource(testNino, TaxYear.current, empRefId), Duration.Inf)

        incomeSource shouldBe payAsYouEarn.incomeSources.get(empRefId)
      }

      "get Income Source return Data for previous year when no data and taxAccountPreviousYearsFlag = true" in {
        enableFlag()

        val empRefId = payAsYouEarn.incomeSources.keys.head

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((testNino, TaxYear.current), payAsYouEarn)
          .futureValue
        testEmploymentHistoryService.cacheService
          .insertOrUpdate((testNino, TaxYear.current.previous), payAsYouEarn)
          .futureValue
        val incomeSource =
          result(
            testEmploymentHistoryService.getIncomeSource(testNino, TaxYear.current.previous, empRefId),
            Duration.Inf
          )

        incomeSource shouldBe payAsYouEarn.incomeSources.get(empRefId)
      }

      "get Income Source return None when no data and taxAccountPreviousYearsFlag = false" in {
        disableFlag()

        val empRefId = "invalidEmpId"

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((testNino, TaxYear.current), payAsYouEarn)
          .futureValue

        val exception = intercept[NotFoundException] {
          result(testEmploymentHistoryService.getIncomeSource(testNino, TaxYear.current, empRefId), Duration.Inf)
        }

        exception.message shouldBe s"IncomeSource not found for NINO ${testNino.nino}, tax year ${TaxYear.current
          .toString()}, and employmentId $empRefId"
      }

      "get Income Source return Data for current year when no data and taxAccountPreviousYearsFlag = false" in {
        disableFlag()

        val empRefId = payAsYouEarn.incomeSources.keys.head

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((testNino, TaxYear.current), payAsYouEarn)
          .futureValue
        val incomeSource =
          result(testEmploymentHistoryService.getIncomeSource(testNino, TaxYear.current, empRefId), Duration.Inf)

        incomeSource shouldBe payAsYouEarn.incomeSources.get(empRefId)
      }

      "get Income Source return Data for previous year when no data and taxAccountPreviousYearsFlag = false" in {
        disableFlag()

        val empRefId = payAsYouEarn.incomeSources.keys.head

        testEmploymentHistoryService.cacheService
          .insertOrUpdate((testNino, TaxYear.current), payAsYouEarn)
          .futureValue
        testEmploymentHistoryService.cacheService
          .insertOrUpdate((testNino, TaxYear.current.previous), payAsYouEarn)
          .futureValue
        val exception = intercept[NotFoundException] {
          result(
            testEmploymentHistoryService.getIncomeSource(testNino, TaxYear.current.previous, empRefId),
            Duration.Inf
          )
        }

        exception.message shouldBe s"IncomeSource not found for NINO ${testNino.nino}, tax year ${TaxYear.current.previous
          .toString()}, and employmentId $empRefId"
      }
    }
  }

}
