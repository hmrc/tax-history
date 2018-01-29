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

import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.api.{CompanyBenefit, Employment, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.audit.{DataEventAuditType, DataEventDetail, DataEventTransaction}
import uk.gov.hmrc.taxhistory.model.nps.{EmploymentStatus, Iabd, NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.RtiDataHelper
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class EmploymentServiceSpec extends PlaySpec with MockitoSugar with TestUtil{

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()
  
  val testEmploymentHistoryService = TestEmploymentHistoryService.createNew()

  val failureResponseJson = Json.parse("""{"reason":"Bad Request"}""")

  val npsEmploymentResponseJson =  Json.parse(""" [{
                             |    "nino": "AA000000",
                             |    "sequenceNumber": 1,
                             |    "worksNumber": "6044041000000",
                             |    "taxDistrictNumber": "531",
                             |    "payeNumber": "J4816",
                             |    "employerName": "Aldi",
                             |    "receivingJobseekersAllowance" : false,
                             |    "otherIncomeSourceIndicator" : false,
                             |    "receivingOccupationalPension": true,
                             |    "startDate": "21/01/2015",
                             |    "employmentStatus":1
                             |    }]
                           """.stripMargin)

  val npsEmploymentResponse = npsEmploymentResponseJson.as[List[NpsEmployment]]

  private val npsEmploymentWithJobSeekerAllowanceJson =  Json.parse(""" [{
                                            |    "nino": "AA000000",
                                            |    "sequenceNumber": 1,
                                            |    "worksNumber": "6044041000000",
                                            |    "taxDistrictNumber": "531",
                                            |    "payeNumber": "J4816",
                                            |    "employerName": "Aldi",
                                            |    "receivingJobseekersAllowance" : true,
                                            |    "otherIncomeSourceIndicator" : false,
                                            |    "receivingOccupationalPension": false,
                                            |    "startDate": "21/01/2015",
                                            |    "employmentStatus":1
                                            |    },
                                            |    {
                                            |    "nino": "AA000000",
                                            |    "sequenceNumber": 1,
                                            |    "worksNumber": "6044041000000",
                                            |    "taxDistrictNumber": "531",
                                            |    "payeNumber": "J4816",
                                            |    "employerName": "Aldi",
                                            |    "receivingJobseekersAllowance" : false,
                                            |    "otherIncomeSourceIndicator" : false,
                                            |    "receivingOccupationalPension": false,
                                            |    "startDate": "21/01/2015",
                                            |    "employmentStatus":1
                                            |    }]
                                          """.stripMargin)

  val npsEmploymentWithJobSeekerAllowance = npsEmploymentWithJobSeekerAllowanceJson.as[List[NpsEmployment]]

  private val npsEmploymentWithOtherIncomeSourceIndicatorJson =  Json.parse(""" [{
                                                                  |    "nino": "AA000000",
                                                                  |    "sequenceNumber": 1,
                                                                  |    "worksNumber": "6044041000000",
                                                                  |    "taxDistrictNumber": "531",
                                                                  |    "payeNumber": "J4816",
                                                                  |    "employerName": "Aldi",
                                                                  |    "receivingJobseekersAllowance" : true,
                                                                  |    "otherIncomeSourceIndicator": false,
                                                                  |    "receivingOccupationalPension": false,
                                                                  |    "startDate": "21/01/2015",
                                                                  |    "employmentStatus":1
                                                                  |    },
                                                                  |    {
                                                                  |    "nino": "AA000000",
                                                                  |    "sequenceNumber": 1,
                                                                  |    "worksNumber": "6044041000000",
                                                                  |    "taxDistrictNumber": "531",
                                                                  |    "payeNumber": "J4816",
                                                                  |    "employerName": "Aldi",
                                                                  |    "receivingJobseekersAllowance" : false,
                                                                  |    "otherIncomeSourceIndicator": true,
                                                                  |    "receivingOccupationalPension": false,
                                                                  |    "startDate": "21/01/2015",
                                                                  |    "employmentStatus":1
                                                                  |    }]
                                                                """.stripMargin)

  val npsEmploymentWithOtherIncomeSourceIndicator = npsEmploymentWithOtherIncomeSourceIndicatorJson.as[List[NpsEmployment]]

  private val npsEmploymentWithJustJobSeekerAllowanceJson =  Json.parse(""" [{
                                                                          |    "nino": "AA000000",
                                                                          |    "sequenceNumber": 1,
                                                                          |    "worksNumber": "6044041000000",
                                                                          |    "taxDistrictNumber": "531",
                                                                          |    "payeNumber": "J4816",
                                                                          |    "employerName": "Aldi",
                                                                          |    "receivingJobseekersAllowance" : true,
                                                                          |    "otherIncomeSourceIndicator": false,
                                                                          |    "receivingOccupationalPension": false,
                                                                          |    "startDate": "21/01/2015",
                                                                          |    "employmentStatus":1
                                                                          |    }]
                                                                        """.stripMargin)

  val npsEmploymentWithJustJobSeekerAllowance = npsEmploymentWithJustJobSeekerAllowanceJson.as[List[NpsEmployment]]

  private val npsEmploymentWithJustOtherIncomeSourceIndicatorJson =  Json.parse(""" [{
                                                                      |    "nino": "AA000000",
                                                                      |    "sequenceNumber": 1,
                                                                      |    "worksNumber": "6044041000000",
                                                                      |    "taxDistrictNumber": "531",
                                                                      |    "payeNumber": "J4816",
                                                                      |    "employerName": "Aldi",
                                                                      |    "receivingJobseekersAllowance" : false,
                                                                      |     "otherIncomeSourceIndicator": true,
                                                                      |     "receivingOccupationalPension": false,
                                                                      |    "startDate": "21/01/2015",
                                                                      |    "employmentStatus":1
                                                                      |    }]
                                                                    """.stripMargin)

  val npsEmploymentWithJustOtherIncomeSourceIndicator = npsEmploymentWithJustOtherIncomeSourceIndicatorJson.as[List[NpsEmployment]]

  val npsEmploymentResponseWithTaxDistrictNumber =  Json.parse(""" [{
                                            |    "nino": "AA000000",
                                            |    "sequenceNumber": 6,
                                            |    "worksNumber": "6044041000000",
                                            |    "taxDistrictNumber": "0531",
                                            |    "payeNumber": "J4816",
                                            |    "employerName": "Aldi",
                                            |    "receivingJobseekersAllowance" : false,
                                            |    "otherIncomeSourceIndicator": false,
                                            |    "receivingOccupationalPension": false,
                                            |    "startDate": "21/01/2015",
                                            |    "employmentStatus":1
                                            |    }]
                                          """.stripMargin)

  lazy val rtiEmploymentResponseJson = loadFile("/json/rti/response/dummyRti.json")
  lazy val rtiEmploymentResponse = rtiEmploymentResponseJson.as[RtiData]
  lazy val rtiDuplicateEmploymentsResponse = loadFile("/json/rti/response/dummyRtiDuplicateEmployments.json")
  lazy val rtiPartialDuplicateEmploymentsResponse = loadFile("/json/rti/response/dummyRtiPartialDuplicateEmployments.json")
  lazy val rtiNonMatchingEmploymentsResponse = loadFile("/json/rti/response/dummyRtiNonMatchingEmployment.json")
  lazy val rtiNoPaymentsResponse = loadFile("/json/rti/response/dummyRtiNoPaymentsResponse.json")
  lazy val npsEmptyEmployments = loadFile("/json/nps/response/emptyEmployments.json")
  lazy val npsGetTaxAccountJson = loadFile("/json/nps/response/GetTaxAccount.json")
  lazy val npsGetTaxAccount = npsGetTaxAccountJson.as[NpsTaxAccount]
  lazy val iabdsResponseJson = loadFile("/json/nps/response/iabds.json")
  lazy val iabdsResponse = iabdsResponseJson.as[List[Iabd]]

  lazy val employmentsJsonResponse = loadFile("/json/nps/response/employments.json")
  lazy val employmentsApiJsonResponse = loadFile("/json/model/api/employments.json")

  val startDate = new LocalDate("2015-01-21")

  "Employment Service" should {
    "successfully get Nps Employments Data" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(npsEmploymentResponse))

      noException shouldBe thrownBy(await(testEmploymentHistoryService.getNpsEmployments(testNino, TaxYear(2016))))
    }

    "return any non success status response from get Nps Employments api" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("")))

      intercept[BadRequestException](await(testEmploymentHistoryService.getNpsEmployments(testNino, TaxYear(2016))))
    }

    "successfully get Rti Employments Data" in {
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(rtiEmploymentResponse))

      val result = await(testEmploymentHistoryService.getRtiEmployments(testNino, TaxYear(2016)))
      result mustBe rtiEmploymentResponse
    }

    "return not found status response from get Nps Employments api" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(Nil))
      intercept[NotFoundException](await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016))))
    }

    "return success status despite failing response from get Rti Employments api when there are nps employments" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("")))
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("")))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("")))

      noException shouldBe thrownBy {
        await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
      }
    }

    "return success response from get Employments" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(iabdsResponse))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(rtiEmploymentResponse))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("")))

      val paye = await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))

      val employments = paye.employments
      employments.size mustBe 1
      employments.head.employerName mustBe "Aldi"
      employments.head.payeReference mustBe "531/J4816"
      employments.head.startDate mustBe startDate
      employments.head.endDate mustBe None

      val payAndTax = paye.payAndTax.map(pMap => pMap(employments.head.employmentId.toString)).get
      payAndTax.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.earlierYearUpdates.size mustBe 1

      val eyu = payAndTax.earlierYearUpdates.head
      eyu.receivedDate mustBe new LocalDate("2016-06-01")
      eyu.taxablePayEYU mustBe BigDecimal(-600.99)
      eyu.taxEYU mustBe BigDecimal(-10.99)

      val benefits = paye.benefits.map(bMap => bMap.get(employments.head.employmentId.toString)).flatten
      benefits.get.size mustBe 2
      benefits.get.head.iabdType mustBe "CarFuelBenefit"
      benefits.get.head.amount mustBe BigDecimal(100)
      benefits.get.last.iabdType mustBe "VanBenefit"
      benefits.get.last.amount mustBe BigDecimal(100)

    }

    "successfully merge rti and nps employment1 data into employment1 list" in {

      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(npsGetTaxAccount))

      val npsEmployments = npsEmploymentResponseJson.as[List[NpsEmployment]]

      val payAsYouEarn = await(testEmploymentHistoryService.mergeAndRetrieveEmployments(testNino, TaxYear.current.previous)(npsEmployments))

      val employment = payAsYouEarn.employments.head
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      val benefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten

      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "531/J4816"
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      employment.receivingOccupationalPension mustBe true
      payAndTax.get.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.get.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.get.earlierYearUpdates.size mustBe 1
      val eyu = payAndTax.get.earlierYearUpdates.head
      eyu.taxablePayEYU mustBe BigDecimal(-600.99)
      eyu.taxEYU mustBe BigDecimal(-10.99)
      eyu.receivedDate mustBe new LocalDate("2016-06-01")
      benefits.get.size mustBe 2
      benefits.get.head.iabdType mustBe "CarFuelBenefit"
      benefits.get.head.amount mustBe BigDecimal(100)
      benefits.get.last.iabdType mustBe "VanBenefit"
      benefits.get.last.amount mustBe BigDecimal(100)

    }


    "successfully exclude nps employment1 data" when {
      "nps receivingJobseekersAllowance is true from list of employments" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(npsEmploymentWithJobSeekerAllowance))
        when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(iabdsResponse))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(rtiEmploymentResponse))
        val payAsYouEarn = await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))

        val employments = payAsYouEarn.employments
        employments.size mustBe 1
      }

      "nps receivingJobseekersAllowance and otherIncomeSourceIndicator is true from list of employments" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(npsEmploymentWithOtherIncomeSourceIndicator))
        when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(iabdsResponse))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(rtiEmploymentResponse))

        intercept[NotFoundException](await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016))))
      }
    }

    "throw not found error" when {
      "nps employments contain single element with receivingJobseekersAllowance attribute is true" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(npsEmploymentWithJustJobSeekerAllowance))
        when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(iabdsResponse))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(rtiEmploymentResponse))

        intercept[NotFoundException](await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016))))
      }

      "nps employments contain single element with npsEmploymentWithJustOtherIncomeSourceIndicator attribute is true" in {
        when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(npsEmploymentWithJustOtherIncomeSourceIndicator))
        when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(iabdsResponse))
        when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(rtiEmploymentResponse))

        intercept[NotFoundException](await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016))))
      }
    }

    "return any non success status response from get Nps Iabds api" in {
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("")))

      intercept[BadRequestException](await(testEmploymentHistoryService.getNpsIabds(testNino,TaxYear(2016))))
    }

    "return none where tax year is not cy-1" in {
      val response = await(testEmploymentHistoryService.getNpsTaxAccount(testNino, TaxYear(2015)))
      response mustBe None
    }

    "return any non success status response from get Nps Tax Account api" in {
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("")))

      intercept[BadRequestException](await(testEmploymentHistoryService.getNpsTaxAccount(testNino,TaxYear(2016))))
    }

    "return not found status response from get Nps Tax Account api" in {
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
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
      val rtiDataHelper = new RtiDataHelper(mockAuditable)

      rtiDataHelper.auditOnlyInRTI(testNino.toString, npsEmployments, rtiEmployments)
      verify (
        mockAuditable,
        times(1))
        .sendDataEvents(any[DataEventTransaction], any[String], any[Map[String, String]],
          any[Seq[DataEventDetail]], any[DataEventAuditType])(hc = any[HeaderCarrier])
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
      val rtiDataHelper = new RtiDataHelper(mockAuditable)

      rtiDataHelper.auditOnlyInRTI(testNino.toString, npsEmployments, rtiEmployments)
      verify (
        mockAuditable,
        times(1))
        .sendDataEvents(any[DataEventTransaction], any[String], any[Map[String, String]],
          any[Seq[DataEventDetail]], any[DataEventAuditType])(hc = any[HeaderCarrier])
    }

    "fetch Employments successfully from cache" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      val testEmployments = Json.parse(
        """ [
          | {
          |      "employmentId": "01318d7c-bcd9-47e2-8c38-551e7ccdfae3",
          |      "startDate": "2016-01-21",
          |      "endDate": "2017-01-01",
          |      "payeReference": "paye-1",
          |      "employerName": "employer-1",
          |      "companyBenefitsURI": "/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/company-benefits",
          |      "payAndTaxURI": "/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/pay-and-tax",
          |      "employmentURI": "/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3",
          |      "receivingOccupationalPension": false,
          |      "employmentStatus":1
          |    },
          |    {
          |      "employmentId": "019f5fee-d5e4-4f3e-9569-139b8ad81a87",
          |      "startDate": "2016-02-22",
          |      "payeReference": "paye-2",
          |      "employerName": "employer-2",
          |      "companyBenefitsURI": "/2014/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87/company-benefits",
          |      "payAndTaxURI": "/2014/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87/pay-and-tax",
          |      "employmentURI": "/2014/employments/019f5fee-d5e4-4f3e-9569-139b8ad81a87",
          |      "receivingOccupationalPension": false,
          |      "employmentStatus":1
          |    }
          |] """.stripMargin).as[List[Employment]]

      when(testEmploymentHistoryService.getFromCache(Matchers.any(),Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(paye))

      val employments = await(testEmploymentHistoryService.getEmployments(Nino("AA000000A"), TaxYear(2014)))
      employments must be (testEmployments)
    }

    "return not found when no employment was returned from cache" in {
      val paye = PayAsYouEarn(employments = Nil)

      when(testEmploymentHistoryService.getFromCache(Matchers.any(),Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(paye))

      intercept[NotFoundException](await(testEmploymentHistoryService.getEmployments(Nino("AA000000A"), TaxYear(2014))))
    }


    "get Employment successfully" in {
      val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      val testEmployment = Json.parse(
        """| {
           |      "employmentId": "01318d7c-bcd9-47e2-8c38-551e7ccdfae3",
           |      "startDate": "2016-01-21",
           |      "endDate": "2017-01-01",
           |      "payeReference": "paye-1",
           |      "employerName": "employer-1",
           |      "companyBenefitsURI": "/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/company-benefits",
           |      "payAndTaxURI": "/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3/pay-and-tax",
           |      "employmentURI": "/2014/employments/01318d7c-bcd9-47e2-8c38-551e7ccdfae3",
           |      "receivingOccupationalPension": false,
           |      "employmentStatus":1
           |    }
           """.stripMargin).as[Employment]

      when(testEmploymentHistoryService.getFromCache(Matchers.any(),Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(paye))

      val employment = await(testEmploymentHistoryService.getEmployment(Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))
      employment must be (testEmployment)
    }


    "get Employment return none" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      when(testEmploymentHistoryService.getFromCache(Matchers.any(),Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(paye))

      intercept[NotFoundException](await(testEmploymentHistoryService.getEmployment(Nino("AA000000A"), TaxYear(2014),"01318d7c-bcd9-47e2-8c38-551e7ccdfae6")))
    }

    "get company benefits from cache successfully" in {
      val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]

      val testCompanyBenefits = Json.parse(
        """| [{
           |      "companyBenefitId": "c9923a63-4208-4e03-926d-7c7c88adc7ee",
           |      "iabdType": "companyBenefitType",
           |      "amount": 12
           |    }]
        """.stripMargin).as[List[CompanyBenefit]]

      when(testEmploymentHistoryService.getFromCache(Matchers.any(),Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(paye))

      val companyBenefits = await(testEmploymentHistoryService.getCompanyBenefits(Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"))

      companyBenefits must be (testCompanyBenefits)
    }

    "return not found when no company benefits returned from cache" in {
      lazy val payeNoBenefits = loadFile("/json/model/api/payeNoCompanyBenefits.json").as[PayAsYouEarn]

      when(testEmploymentHistoryService.getFromCache(Matchers.any(),Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(payeNoBenefits))

      intercept[NotFoundException](await(testEmploymentHistoryService.getCompanyBenefits(Nino("AA000000A"), TaxYear(2014), "01318d7c-bcd9-47e2-8c38-551e7ccdfae3")))
    }
  }
}