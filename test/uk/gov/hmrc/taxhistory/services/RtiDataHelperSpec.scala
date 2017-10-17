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
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.api.PayAsYouEarn
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.RtiDataHelper
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future


class RtiDataHelperSpec extends PlaySpec with MockitoSugar with TestUtil{


  implicit val hc = HeaderCarrier()
  val testNino = randomNino()



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


  lazy val rtiEmploymentResponse = loadFile("/json/rti/response/dummyRti.json")
  lazy val rtiPartialDuplicateEmploymentsResponse = loadFile("/json/rti/response/dummyRtiPartialDuplicateEmployments.json")
  lazy val rtiNonMatchingEmploymentsResponse = loadFile("/json/rti/response/dummyRtiNonMatchingEmployment.json")


  val startDate = new LocalDate("2015-01-21")


  "RtiDataHelper" should {


    "successfully merge if there are multiple matching rti employments for a single nps employment1 but single match on currentPayId" in {
      val rtiData = rtiPartialDuplicateEmploymentsResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]
       val rtiDataHelper = new RtiDataHelper(rtiData)
      val rtiEmployments = rtiDataHelper.getMatchedRtiEmployments(npsEmployments.head){
        Future.successful(List())
      }
      rtiEmployments.size mustBe 1

    }

    "return Nil constructed list if there are zero matching rti employments for a single nps employment1" in {
      val rtiData = rtiNonMatchingEmploymentsResponse.as[RtiData]
      val npsEmployments = npsEmploymentResponse.as[List[NpsEmployment]]
      val rtiDataHelper = new RtiDataHelper(rtiData)
      val rtiEmployments = rtiDataHelper.getMatchedRtiEmployments(npsEmployments.head){
        Future.successful(List())
      }
      rtiEmployments.size mustBe 0
    }



    "get rti payments from employment1 data" in {
      val rtiData = rtiEmploymentResponse.as[RtiData]
      val rtiDataHelper = new RtiDataHelper(rtiData)

      val paymentInfo =rtiDataHelper.getRtiPayment(rtiData.employments)
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
  }
}
