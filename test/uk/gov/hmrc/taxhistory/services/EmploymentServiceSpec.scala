/*
 * Copyright 2017 HM Revenue & Customs
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
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment}
import uk.gov.hmrc.taxhistory.model.taxhistory._
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class EmploymentServiceSpec extends PlaySpec with MockitoSugar with TestUtil{
  private val mockNpsConnector= mock[NpsConnector]
  private val mockRtiDataConnector= mock[RtiConnector]

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()
  object TestEmploymentService extends EmploymentHistoryService {
    override def npsConnector: NpsConnector = mockNpsConnector
    override def rtiConnector: RtiConnector = mockRtiDataConnector
  }

  val failureResponseJson = Json.parse("""{"reason":"Bad Request"}""")

  val npsEmploymentResponse =  Json.parse(""" [{
                             |    "nino": "AA000000",
                             |    "sequenceNumber": 1,
                             |    "worksNumber": "6044041000000",
                             |    "taxDistrictNumber": "531",
                             |    "payeNumber": "J4816",
                             |    "employerName": "Aldi",
                             |    "receivingJobseekersAllowance" : false,
                             |    "startDate": "21/01/2015"
                             |    }]
                           """.stripMargin)

  private val npsEmploymentWithJobSeekerAllowance =  Json.parse(""" [{
                                            |    "nino": "AA000000",
                                            |    "sequenceNumber": 1,
                                            |    "worksNumber": "6044041000000",
                                            |    "taxDistrictNumber": "531",
                                            |    "payeNumber": "J4816",
                                            |    "employerName": "Aldi",
                                            |    "receivingJobseekersAllowance" : true,
                                            |    "startDate": "21/01/2015"
                                            |    },
                                            |    {
                                            |    "nino": "AA000000",
                                            |    "sequenceNumber": 1,
                                            |    "worksNumber": "6044041000000",
                                            |    "taxDistrictNumber": "531",
                                            |    "payeNumber": "J4816",
                                            |    "employerName": "Aldi",
                                            |    "receivingJobseekersAllowance" : false,
                                            |    "startDate": "21/01/2015"
                                            |    }]
                                          """.stripMargin)

  private val npsEmploymentWithJustJobSeekerAllowance =  Json.parse(""" [{
                                                                          |    "nino": "AA000000",
                                                                          |    "sequenceNumber": 1,
                                                                          |    "worksNumber": "6044041000000",
                                                                          |    "taxDistrictNumber": "531",
                                                                          |    "payeNumber": "J4816",
                                                                          |    "employerName": "Aldi",
                                                                          |    "receivingJobseekersAllowance" : true,
                                                                          |    "startDate": "21/01/2015"
                                                                          |    }]
                                                                        """.stripMargin)
  val npsEmploymentResponseWithTaxDistrictNumber =  Json.parse(""" [{
                                            |    "nino": "AA000000",
                                            |    "sequenceNumber": 6,
                                            |    "worksNumber": "6044041000000",
                                            |    "taxDistrictNumber": "0531",
                                            |    "payeNumber": "J4816",
                                            |    "employerName": "Aldi",
                                            |    "receivingJobseekersAllowance" : false,
                                            |    "startDate": "21/01/2015"
                                            |    }]
                                          """.stripMargin)

  lazy val rtiEmploymentResponse = loadFile("/json/rti/response/dummyRti.json")
  lazy val rtiDuplicateEmploymentsResponse = loadFile("/json/rti/response/dummyRtiDuplicateEmployments.json")
  lazy val rtiPartialDuplicateEmploymentsResponse = loadFile("/json/rti/response/dummyRtiPartialDuplicateEmployments.json")
  lazy val rtiNonMatchingEmploymentsResponse = loadFile("/json/rti/response/dummyRtiNonMatchingEmployment.json")
  lazy val rtiNoPaymentsResponse = loadFile("/json/rti/response/dummyRtiNoPaymentsResponse.json")
  lazy val npsEmptyEmployments = loadFile("/json/nps/response/emptyEmployments.json")
  lazy val iabdsJsonResponse = loadFile("/json/nps/response/iabds.json")
  lazy val iabdsTotalBenfitInKindJsonResponse = loadFile("/json/nps/response/iabdsTotalBIK.json")
  lazy val iabdsBenfitInKindJsonResponse = loadFile("/json/nps/response/iabdsBIK.json")

  lazy val employmentsJsonResponse = loadFile("/json/nps/response/employments.json")

  val startDate = new LocalDate("2015-01-21")


  "Employment Service" should {
    "successfully get Nps Employments Data" in {
      when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentResponse))))

      val eitherResponse = await(TestEmploymentService.getNpsEmployments(testNino, TaxYear(2016)))
      assert(eitherResponse.isRight)
      eitherResponse.right.get mustBe a[List[NpsEmployment]]

    }

    "handle any non success status response from get Nps Employments" in {
      when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(npsEmploymentResponse))))

      val eitherResponse = await(TestEmploymentService.getNpsEmployments(testNino, TaxYear(2016)))
      assert(eitherResponse.isLeft)
      eitherResponse.left.get mustBe a[HttpResponse]
    }

    "successfully get Rti Employments Data" in {
      when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(rtiEmploymentResponse))))

      val eitherResponse = await(TestEmploymentService.getRtiEmployments(testNino, TaxYear(2016)))
      assert(eitherResponse.isRight)
      eitherResponse.right.get mustBe a[RtiData]
    }

    "handle any non success status response from get Rti Employments" in {
      when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(rtiEmploymentResponse))))

      val eitherResponse = await(TestEmploymentService.getRtiEmployments(testNino, TaxYear(2016)))
      assert(eitherResponse.isLeft)
      eitherResponse.left.get mustBe a[HttpResponse]
    }

    "return any non success status response from get Nps Employments api" in {
      when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(npsEmploymentResponse))))
      val response =  await(TestEmploymentService.getEmploymentHistory(testNino.toString(),2016))
      response mustBe a[HttpResponse]
      response.status mustBe BAD_REQUEST
    }

    "return not found status response from get Nps Employments api" in {
      when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(JsArray(Seq.empty)))))
      val response =  await(TestEmploymentService.getEmploymentHistory(testNino.toString(),2016))
      response mustBe a[HttpResponse]
      response.status mustBe NOT_FOUND
    }

    "return success status despite failing response from get Rti Employments api when there are nps employments" in {
      when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentResponse))))
      when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(rtiEmploymentResponse))))
      when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(iabdsJsonResponse))))
      val response =  await(TestEmploymentService.getEmploymentHistory(testNino.toString(),2016))
      response mustBe a[HttpResponse]
      response.status mustBe OK
    }

    "return success response from get Employments" in {
      when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentResponse))))
      when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(iabdsJsonResponse))))
      when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(rtiEmploymentResponse))))
      val response =  await(TestEmploymentService.getEmploymentHistory(testNino.toString(),2016))
      response mustBe a[HttpResponse]
      response.status mustBe OK
      val payAsYouEarnDetails = response.json.as[PayAsYouEarnDetails]
      val employments = payAsYouEarnDetails.employments
      employments.size mustBe 1
      employments.head.employerName mustBe "Aldi"
      employments.head.payeReference mustBe "531/J4816"
      employments.head.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      employments.head.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      employments.head.earlierYearUpdates mustBe List(EarlierYearUpdate(-600.99,-10.99,LocalDate.parse("2016-06-01")))
      employments.head.startDate mustBe startDate
      employments.head.companyBenefits mustBe List(CompanyBenefit("Car Fuel Benefit",100,"CarFuelBenefit"), CompanyBenefit("Van Benefit",100,"VanBenefit"))
      employments.head.endDate mustBe None
      println(Json.toJson(employments.head.companyBenefits))
    }

    "successfully merge rti and nps employment data into employment list" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]

      val response = await(TestEmploymentService.getPayAsYouEarnDetails(testNino, TaxYear(2016))(npsEmployments))
      response mustBe a[HttpResponse]

      response.status mustBe OK
      val payAsYouEarnDetails = response.json.as[PayAsYouEarnDetails]

      val employments = payAsYouEarnDetails.employments

      employments.head.employerName mustBe "Aldi"
      employments.head.payeReference mustBe "531/J4816"
      employments.head.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      employments.head.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      employments.head.earlierYearUpdates mustBe List(EarlierYearUpdate(-600.99,-10.99,LocalDate.parse("2016-06-01")))
      employments.head.startDate mustBe startDate
      employments.head.endDate mustBe None
    }

    "successfully exclude nps employment data" when {
      "nps receivingJobseekersAllowance is true form list of employments" in {
        when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentWithJobSeekerAllowance))))
        when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(iabdsJsonResponse))))
        when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(rtiEmploymentResponse))))
        val response = await(TestEmploymentService.getEmploymentHistory(testNino.toString(), 2016))
        response mustBe a[HttpResponse]
        response.status mustBe OK
        val payAsYouEarnDetails = response.json.as[PayAsYouEarnDetails]
        val employments = payAsYouEarnDetails.employments
        employments.size mustBe 1
        payAsYouEarnDetails.allowances mustBe List(Allowance("Flat Rate Job Expenses",200, "FlatRateJobExpenses"))
      }
    }

    "throw not found error" when {
      "nps employments contain single element with receivingJobseekersAllowance attribute is true" in {
        when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentWithJustJobSeekerAllowance))))
        when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(iabdsJsonResponse))))
        when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(rtiEmploymentResponse))))
        val response = await(TestEmploymentService.getEmploymentHistory(testNino.toString(), 2016))
        response mustBe a[HttpResponse]
        response.status mustBe NOT_FOUND
      }
    }

    "successfully merge if there are multiple matching rti employments for a single nps employment but single match on currentPayId" in {
      val rtiData = rtiPartialDuplicateEmploymentsResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]

      val rtiEmployments = TestEmploymentService.getMatchedRtiEmployments(rtiData,npsEmployments.head)
      rtiEmployments.size mustBe 1

    }

    "return Nil constructed list if there are zero matching rti employments for a single nps employment" in {
      val rtiData = rtiNonMatchingEmploymentsResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]

      val rtiEmployments =TestEmploymentService.getMatchedRtiEmployments(rtiData , npsEmployments.head)
      rtiEmployments.size mustBe 0
    }

    "correctly compare matching numerical taxDistrictNumbers" in {
      TestEmploymentService.formatString("12") mustBe "12"
    }
    "correctly compare matching alphabetical taxDistrictNumbers" in {
      TestEmploymentService.formatString("ABC") mustBe "ABC"
    }

    "correctly compare taxDistrictNumbers as integers if one has a leading zero" in {
      TestEmploymentService.formatString("073") mustBe "73"
    }
    "not match different taxDistrictNumbers" in {
      TestEmploymentService.formatString("330") mustBe "330"
    }
    "not match taxDistrictNumbers if one is blank" in {
      TestEmploymentService.formatString("")  mustBe ""
    }
    "return any non success status response from get Nps Iabds api" in {
      when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(failureResponseJson))))
      val response =  await(TestEmploymentService.getNpsIabds(testNino,TaxYear(2016)))
      response mustBe a[Either[HttpResponse,List[Iabd]]]
      response.left.get.status mustBe BAD_REQUEST

    }

    "return not found status response from get Nps Iabds api" in {
      when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(JsArray(Seq.empty)))))
      val response =  await(TestEmploymentService.getNpsIabds(testNino,TaxYear(2016)))
      response mustBe a[Either[HttpResponse,List[Iabd]]]
      response.left.get.status mustBe NOT_FOUND

    }

   "Return a filtered Iabds from  List of Nps Iabds" in {
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      iabds mustBe a [List[Iabd]]
      val iabdsFiltered = TestEmploymentService.getRawCompanyBenefits(iabds)
      iabdsFiltered.size mustBe 4
      iabdsFiltered.toString() contains  ("FlatRateJobExpenses") mustBe false
      iabdsFiltered.toString() contains  ("VanBenefit")  mustBe true
      iabdsFiltered.toString() contains  ("CarFuelBenefit")  mustBe true

    }


    "Return a matched iabds  from  List of employments" in {
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val employments = npsEmploymentResponse.as[List[NpsEmployment]]

      iabds mustBe a [List[Iabd]]
      val matchedIabds = TestEmploymentService.getMatchedCompanyBenefits(iabds,employments.head)
      matchedIabds.size mustBe 2
      println( matchedIabds.toString())
      matchedIabds.toString() contains  ("VanBenefit") mustBe true
      matchedIabds.toString() contains  ("CarFuelBenefit") mustBe true

    }

    "Return only Allowances from  List of Nps Iabds" in {
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      iabds mustBe a [List[Iabd]]
      val iabdsFiltered = TestEmploymentService.getRawAllowances(iabds)
      iabdsFiltered.size mustBe 1
      iabdsFiltered.toString() contains  ("FlatRateJobExpenses") mustBe true

    }

    "Build employment from rti ,nps employment and Iabd data" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val employment=TestEmploymentService.buildEmployment(Some(rtiData.employments),Some(iabds), npsEmployments.head)
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      employment.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      employment.earlierYearUpdates mustBe List(EarlierYearUpdate(-600.99,-10.99,LocalDate.parse("2016-06-01")))
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      employment.companyBenefits.size mustBe  8
    }

    "Build employment when there is no  data for rti and Iabd" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val employment=TestEmploymentService.buildEmployment(None,None, npsEmployments.head)
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.taxablePayTotal mustBe None
      employment.taxTotal mustBe None
      employment.earlierYearUpdates mustBe List()
      employment.companyBenefits.size mustBe  0
      employment.startDate mustBe startDate
      employment.endDate mustBe None
    }
    "Build employment when there is data for rti is Nil " in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val employment=TestEmploymentService.buildEmployment(None,Some(iabds), npsEmployments.head)
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.taxablePayTotal mustBe None
      employment.taxTotal mustBe None
      employment.earlierYearUpdates mustBe List()
      employment.companyBenefits.size mustBe  8
      employment.startDate mustBe startDate
      employment.endDate mustBe None
    }

    "Build employment when data for rti is None or Null" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val employment=TestEmploymentService.buildEmployment(None,Some(iabds), npsEmployments.head)
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.taxablePayTotal mustBe None
      employment.taxTotal mustBe None
      employment.earlierYearUpdates mustBe List()
      employment.companyBenefits.size mustBe  8
      employment.startDate mustBe startDate
      employment.endDate mustBe None
    }
    "Build employment when there is data for Iabd is None or Null" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val employment=TestEmploymentService.buildEmployment(Some(rtiData.employments),None, npsEmployments.head)
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      employment.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      employment.earlierYearUpdates mustBe List(EarlierYearUpdate(-600.99,-10.99,LocalDate.parse("2016-06-01")))
      employment.companyBenefits.size mustBe  0
      employment.startDate mustBe startDate
      employment.endDate mustBe None
    }
    "Build employment when there is no  data for Iabd" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val employment=TestEmploymentService.buildEmployment(Some(rtiData.employments),Some(Nil), npsEmployments.head)
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      employment.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      employment.earlierYearUpdates mustBe List(EarlierYearUpdate(-600.99,-10.99,LocalDate.parse("2016-06-01")))
      employment.companyBenefits.size mustBe  0
      employment.startDate mustBe startDate
      employment.endDate mustBe None
    }

    "Get CompanyBenfits from Iabd data and ignore Benefit In Kind (type 28)" in {

      val iabds = iabdsBenfitInKindJsonResponse.as[List[Iabd]]
      val companyBenefits=TestEmploymentService.getCompanyBenefits(iabds)
      companyBenefits.size mustBe  7
    }

    "Get CompanyBenfits from Iabd data Benefit In Kind of type 28(Total Benefit In Kind)" in {

      val iabds = iabdsTotalBenfitInKindJsonResponse.as[List[Iabd]]
      val companyBenefits=TestEmploymentService.getCompanyBenefits(iabds)
      companyBenefits.size mustBe  2
    }

    "Total Benefit In Kind  from Iabds list should return true if There is only BIK which is type 28" in {
      val iabds = iabdsTotalBenfitInKindJsonResponse.as[List[Iabd]]
      val bik=TestEmploymentService.isTotalBenefitInKind(iabds)
       bik  mustBe  true
    }
    "Total Benefit In Kind  from Iabds list should return false if There is any BIK which is not type 28" in {
      val iabds = iabdsBenfitInKindJsonResponse.as[List[Iabd]]
      val bik=TestEmploymentService.isTotalBenefitInKind(iabds)
      bik  mustBe  false
    }

    "get rti payments from employment data" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val paymentInfo =TestEmploymentService.getRtiPayment(rtiData.employments)
      paymentInfo._1 mustBe Some(BigDecimal.valueOf(20000.00))
      paymentInfo._2 mustBe Some(BigDecimal.valueOf(1880.00))
    }


  }


}
