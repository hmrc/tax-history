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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector

import uk.gov.hmrc.taxhistory.model.api.{Employment, EarlierYearUpdate, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.RtiDataHelper
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class EmploymentServiceSpec extends PlaySpec with MockitoSugar with TestUtil{
  private val mockNpsConnector= mock[NpsConnector]
  private val mockRtiDataConnector= mock[RtiConnector]
  private val mockAudit= mock[Audit]
  private val mockCache = mock[TaxHistoryCacheService]

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()
  object TestEmploymentService extends EmploymentHistoryService {
    override def npsConnector: NpsConnector = mockNpsConnector
    override def rtiConnector: RtiConnector = mockRtiDataConnector

    override def cacheService: TaxHistoryCacheService = mockCache

    override def audit: Audit = mockAudit
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
                             |    "otherIncomeSourceIndicator" : false,
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
                                            |    "otherIncomeSourceIndicator" : false,
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
                                            |    "otherIncomeSourceIndicator" : false,
                                            |    "startDate": "21/01/2015"
                                            |    }]
                                          """.stripMargin)


  private val npsEmploymentWithOtherIncomeSourceIndicator =  Json.parse(""" [{
                                                                  |    "nino": "AA000000",
                                                                  |    "sequenceNumber": 1,
                                                                  |    "worksNumber": "6044041000000",
                                                                  |    "taxDistrictNumber": "531",
                                                                  |    "payeNumber": "J4816",
                                                                  |    "employerName": "Aldi",
                                                                  |    "receivingJobseekersAllowance" : true,
                                                                  |    "otherIncomeSourceIndicator": false,
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
                                                                  |    "otherIncomeSourceIndicator": true,
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
                                                                          |    "otherIncomeSourceIndicator": false,
                                                                          |    "startDate": "21/01/2015"
                                                                          |    }]
                                                                        """.stripMargin)

  private val npsEmploymentWithJustOtherIncomeSourceIndicator =  Json.parse(""" [{
                                                                      |    "nino": "AA000000",
                                                                      |    "sequenceNumber": 1,
                                                                      |    "worksNumber": "6044041000000",
                                                                      |    "taxDistrictNumber": "531",
                                                                      |    "payeNumber": "J4816",
                                                                      |    "employerName": "Aldi",
                                                                      |    "receivingJobseekersAllowance" : false,
                                                                      |     "otherIncomeSourceIndicator": true,
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
                                            |    "otherIncomeSourceIndicator": false,
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

  lazy val employmentsJsonResponse = loadFile("/json/nps/response/employments.json")
  lazy val employmentsApiJsonResponse = loadFile("/json/model/api/employments.json")

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
      val response =  await(TestEmploymentService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
      response mustBe a[HttpResponse]
      response.status mustBe BAD_REQUEST
    }

    "return not found status response from get Nps Employments api" in {

      when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(JsArray(Seq.empty)))))
      val response =  await(TestEmploymentService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
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
      val response =  await(TestEmploymentService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
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
      val response =  await(TestEmploymentService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
      response mustBe a[HttpResponse]
      response.status mustBe OK
      val payAsYouEarn = response.json.as[PayAsYouEarn]
      val employments = payAsYouEarn.employments
      employments.size mustBe 1
      employments.head.employerName mustBe "Aldi"
      employments.head.payeReference mustBe "531/J4816"
      // employments.head.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      // employments.head.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      // employments.head.earlierYearUpdates mustBe List(EarlierYearUpdate(-600.99,-10.99,LocalDate.parse("2016-06-01")))
      employments.head.startDate mustBe startDate
      // employments.head.companyBenefits mustBe List(CompanyBenefit("Car Fuel Benefit",100,"CarFuelBenefit"), CompanyBenefit("Van Benefit",100,"VanBenefit"))
      employments.head.endDate mustBe None
    }


//    "return success response from get Employments with getEmployments" in {
//      when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
//        .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentResponse))))
//      when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
//        .thenReturn(Future.successful(HttpResponse(OK, Some(iabdsJsonResponse))))
//      when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
//        .thenReturn(Future.successful(HttpResponse(OK, Some(rtiEmploymentResponse))))
//
//      when(mockCache.getFromCacheOrElse(Matchers.any())(Matchers.any(), Matchers.any()))
//        .thenReturn(Future.successful(Some(npsEmploymentResponse)))
//
//      val response =  await(TestEmploymentService.getEmployments(testNino.nino,2016))
//      response mustBe a[HttpResponse]
//      response.status mustBe OK
//      val payAsYouEarn = response.json.as[PayAsYouEarn]
//      val employments = payAsYouEarn.employments
//      employments.size mustBe 1
//      employments.head.employerName mustBe "Aldi"
//      employments.head.payeReference mustBe "531/J4816"
//     // employments.head.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
//     // employments.head.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
//     // employments.head.earlierYearUpdates mustBe List(EarlierYearUpdate(-600.99,-10.99,LocalDate.parse("2016-06-01")))
//      employments.head.startDate mustBe startDate
//     // employments.head.companyBenefits mustBe List(CompanyBenefit("Car Fuel Benefit",100,"CarFuelBenefit"), CompanyBenefit("Van Benefit",100,"VanBenefit"))
//      employments.head.endDate mustBe None
//    }






    "successfully merge rti and nps employment1 data into employment1 list" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]

      val response = await(TestEmploymentService.mergeAndRetrieveEmployments(testNino, TaxYear(2016))(npsEmployments))
      response mustBe a[HttpResponse]

      response.status mustBe OK
      val payAsYouEarn = response.json.as[PayAsYouEarn]
      val employments = payAsYouEarn.employments

      employments.head.employerName mustBe "Aldi"
      employments.head.payeReference mustBe "531/J4816"
     // employments.head.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
     // employments.head.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
     // employments.head.earlierYearUpdates mustBe List(EarlierYearUpdate(-600.99,-10.99,LocalDate.parse("2016-06-01")))
      employments.head.startDate mustBe startDate
      employments.head.endDate mustBe None
    }

    "successfully exclude nps employment1 data" when {
      "nps receivingJobseekersAllowance is true form list of employments" in {
        when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentWithJobSeekerAllowance))))
        when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(iabdsJsonResponse))))
        when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(rtiEmploymentResponse))))
        val response = await(TestEmploymentService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
        response mustBe a[HttpResponse]
        response.status mustBe OK
        val payAsYouEarn = response.json.as[PayAsYouEarn]
        val employments = payAsYouEarn.employments
        employments.size mustBe 1
      }

      "nps receivingJobseekersAllowance and otherIncomeSourceIndicator is true form list of employments" in {
        when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentWithOtherIncomeSourceIndicator))))
        when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(iabdsJsonResponse))))
        when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(rtiEmploymentResponse))))
        val response = await(TestEmploymentService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
        response mustBe a[HttpResponse]
        response.status mustBe NOT_FOUND
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
        val response = await(TestEmploymentService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
        response mustBe a[HttpResponse]
        response.status mustBe NOT_FOUND
      }

      "nps employments contain single element with npsEmploymentWithJustOtherIncomeSourceIndicator attribute is true" in {
        when(mockNpsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentWithJustOtherIncomeSourceIndicator))))
        when(mockNpsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(iabdsJsonResponse))))
        when(mockRtiDataConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(OK, Some(rtiEmploymentResponse))))
        val response = await(TestEmploymentService.retrieveEmploymentsDirectFromSource(testNino,TaxYear(2016)))
        response mustBe a[HttpResponse]
        response.status mustBe NOT_FOUND
      }
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


    "Build employment1 from rti ,nps employment1 and Iabd data" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val payAsYouEarn=TestEmploymentService.buildPayAsYouEarnList(Some(rtiData.employments),Some(iabds), npsEmployments.head)
      val employment = payAsYouEarn.employments.head
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      payAndTax.get.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.get.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.get.earlierYearUpdates.size mustBe 1
      payAsYouEarn.employments.head.startDate mustBe startDate
      payAsYouEarn.employments.head.endDate mustBe None
      val companyBenefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten
      companyBenefits.get.size mustBe  8
    }

    "Build employment1 when there is no  data for rti and Iabd" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val payAsYouEarn=TestEmploymentService.buildPayAsYouEarnList(None,None, npsEmployments.head)
      val employment = payAsYouEarn.employments.head
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      payAndTax mustBe None
      val companyBenefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten
      companyBenefits mustBe  None

    }
    "Build employment1 when there is data for rti is Nil " in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val payAsYouEarn=TestEmploymentService.buildPayAsYouEarnList(None,Some(iabds), npsEmployments.head)
      val employment = payAsYouEarn.employments.head
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      payAndTax mustBe None
      val companyBenefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten
      companyBenefits.get.size mustBe  8

    }

    "Build employment1 when there is data for Iabd is None or Null" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val payAsYouEarn=TestEmploymentService.buildPayAsYouEarnList(Some(rtiData.employments),None, npsEmployments.head)
      val employment = payAsYouEarn.employments.head
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      payAndTax.get.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.get.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.get.earlierYearUpdates.size mustBe 1
      val companyBenefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten
      companyBenefits mustBe None

    }
    "Build employment1 when there is no  data for Iabd" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponseWithTaxDistrictNumber.as[List[NpsEmployment]]
      val iabds = iabdsJsonResponse.as[List[Iabd]]

      val payAsYouEarn=TestEmploymentService.buildPayAsYouEarnList(Some(rtiData.employments),Some(Nil), npsEmployments.head)
      val employment = payAsYouEarn.employments.head
      employment.employerName mustBe "Aldi"
      employment.payeReference mustBe "0531/J4816"
      employment.startDate mustBe startDate
      employment.endDate mustBe None
      val payAndTax = payAsYouEarn.payAndTax.map(pMap => pMap.get(employment.employmentId.toString)).flatten
      payAndTax.get.taxablePayTotal mustBe Some(BigDecimal.valueOf(20000.00))
      payAndTax.get.taxTotal mustBe Some(BigDecimal.valueOf(1880.00))
      payAndTax.get.earlierYearUpdates.size mustBe 1
      val companyBenefits = payAsYouEarn.benefits.map(bMap => bMap.get(employment.employmentId.toString)).flatten
      companyBenefits mustBe  None
    }
    "get rti payments from employment1 data" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val paymentInfo =TestEmploymentService.getRtiPayment(rtiData.employments)
      paymentInfo._1 mustBe Some(BigDecimal.valueOf(20000.00))
      paymentInfo._2 mustBe Some(BigDecimal.valueOf(1880.00))
    }

    "get onlyRtiEmployments  from List of Rti employments and List Nps Employments" in {
      val rtiEmployment1 = RtiEmployment(1,"offNo1","ref1",None,Nil,Nil)
      val rtiEmployment2 = RtiEmployment(5,"offNo5","ref5",None,Nil,Nil)
      val rtiEmployment3 = RtiEmployment(3,"offNo3","ref3",None,Nil,Nil)
      val rtiEmployment4 = RtiEmployment(4,"offNo4","ref4",None,Nil,Nil)

      val rtiEmployments = List(rtiEmployment1,rtiEmployment2,rtiEmployment3,rtiEmployment4)

      val npsEmployment1 = NpsEmployment(randomNino.toString(),1,"offNo1","ref1","empname1",None,false,false,LocalDate.now(),None)
      val npsEmployment2 = NpsEmployment(randomNino.toString(),2,"offNo2","ref2","empname2",None,false,false,LocalDate.now(),None)
      val npsEmployment3 = NpsEmployment(randomNino.toString(),3,"offNo3","ref3","empname3",None,false,false,LocalDate.now(),None)
      val npsEmployments = List(npsEmployment1,npsEmployment2,npsEmployment3)
      val rtiData = RtiData("QQ0000002", rtiEmployments)
      val rtiDataHelper = new RtiDataHelper(rtiData)


      val onlyRtiEmployments = rtiDataHelper.onlyInRTI(npsEmployments)
      onlyRtiEmployments.size mustBe 2
    }

    "get onlyRtiEmployments must be size 0 when all the Rti employments are matched to the Nps Employments" in {
      val rtiEmployment1 = RtiEmployment(1,"offNo1","ref1",None,Nil,Nil)
      val rtiEmployment2 = RtiEmployment(2,"offNo2","ref2",None,Nil,Nil)
      val rtiEmployment3 = RtiEmployment(3,"offNo3","ref3",None,Nil,Nil)

      val rtiEmployments = List(rtiEmployment1,rtiEmployment2,rtiEmployment3)

      val npsEmployment1 = NpsEmployment(randomNino.toString(),1,"offNo1","ref1","empname1",None,false,false,LocalDate.now(),None)
      val npsEmployment2 = NpsEmployment(randomNino.toString(),2,"offNo2","ref2","empname2",None,false,false,LocalDate.now(),None)
      val npsEmployment3 = NpsEmployment(randomNino.toString(),3,"offNo3","ref3","empname3",None,false,false,LocalDate.now(),None)
      val npsEmployments = List(npsEmployment1,npsEmployment2,npsEmployment3)
      val rtiData = RtiData("QQ0000002", rtiEmployments)
      val rtiDataHelper = new RtiDataHelper(rtiData)
      val onlyRtiEmployments = rtiDataHelper.onlyInRTI(npsEmployments)
      onlyRtiEmployments.size mustBe 0
    }

    "get Employments successfully" in {
      lazy val payeJson = loadFile("/json/model/api/paye.json")

      val employmentJson = Json.parse(
        """ [
          | {
          |      "employmentId": "01318d7c-bcd9-47e2-8c38-551e7ccdfae3",
          |      "startDate": "2016-01-21",
          |      "endDate": "2017-01-01",
          |      "payeReference": "paye-1",
          |      "employerName": "employer-1"
          |    },
          |    {
          |      "employmentId": "019f5fee-d5e4-4f3e-9569-139b8ad81a87",
          |      "startDate": "2016-02-22",
          |      "payeReference": "paye-2",
          |      "employerName": "employer-2"
          |    }
          |] """.stripMargin)

      when(TestEmploymentService.getFromCache(Matchers.any(),Matchers.any(), Matchers.any()))
              .thenReturn(Future.successful(Some(payeJson)))

      val result = await(TestEmploymentService.getEmployments("AA000000A", 2014))
      result.json must be (employmentJson)
    }
  }
}
