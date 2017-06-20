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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.taxhistory.connectors.nps.EmploymentsConnector
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

/**
  * Created by shailesh on 20/06/17.
  */

class EmploymentServiceSpec extends PlaySpec with MockitoSugar{
  private val mockEmploymentConnector= mock[EmploymentsConnector]

  implicit val hc = HeaderCarrier()
  object TestEmploymentService extends EmploymentHistoryService {
    override def employmentsConnector: EmploymentsConnector = mockEmploymentConnector
  }

  val employmentResponse =  Json.parse(""" [{
                             |    "nino": "AA000000",
                             |    "sequenceNumber": 6,
                             |    "worksNumber": "00191048716",
                             |    "taxDistrictNumber": "846",
                             |    "payeNumber": "T2PP",
                             |    "employerName": "Aldi"
                             |    }]
                           """.stripMargin)


  "Get Employments Data" in {
    when(mockEmploymentConnector.getEmployments(Matchers.any(),Matchers.any())(Matchers.any[HeaderCarrier]))
      .thenReturn(Future.successful(HttpResponse(OK, Some(employmentResponse))))

    val eitherResponse = await(TestEmploymentService.getNpsEmployments("AA000000A", TaxYear(2016)))
    assert(eitherResponse.isRight)
    eitherResponse.right.get mustBe a [List[NpsEmployment]]

  }

}
