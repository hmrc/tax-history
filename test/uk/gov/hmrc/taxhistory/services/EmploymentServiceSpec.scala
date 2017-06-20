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

import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.EmploymentsConnector
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

/**
  * Created by shailesh on 20/06/17.
  */

class EmploymentServiceSpec extends PlaySpec with MockitoSugar with TestUtil{
  private val mockEmploymentConnector= mock[EmploymentsConnector]
  private val mockRtiDataConnector= mock[RtiConnector]

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()
  object TestEmploymentService extends EmploymentHistoryService {
    override def employmentsConnector: EmploymentsConnector = mockEmploymentConnector
    override def rtiConnector: RtiConnector = mockRtiDataConnector
  }

  val npsEmploymentResponse =  Json.parse(""" [{
                             |    "nino": "AA000000",
                             |    "sequenceNumber": 6,
                             |    "worksNumber": "00191048716",
                             |    "taxDistrictNumber": "846",
                             |    "payeNumber": "T2PP",
                             |    "employerName": "Aldi"
                             |    }]
                           """.stripMargin)
  lazy val rtiEmploymentResponse = loadFile("/json/rti/response/dummyRti.json")


  "Employment Service" should {
    "successfully get Nps Employments Data" in {
      when(mockEmploymentConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(OK, Some(npsEmploymentResponse))))

      val eitherResponse = await(TestEmploymentService.getNpsEmployments(testNino, TaxYear(2016)))
      assert(eitherResponse.isRight)
      eitherResponse.right.get mustBe a[List[NpsEmployment]]

    }

    "handle any non success status response from get Nps Employments" in {
      when(mockEmploymentConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
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
  }


}
