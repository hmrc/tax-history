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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.api.Allowance
import uk.gov.hmrc.taxhistory.model.nps.Iabd
import uk.gov.hmrc.taxhistory.model.utils.TestUtil


class IabdsHelperSpec extends PlaySpec with MockitoSugar with TestUtil{
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

val onlyIabdJson = """[{
                     |    "nino": "QQ000003",
                     |    "sequenceNumber": 201700055,
                     |    "taxYear": 2017,
                     |    "type": 35,
                     |    "source": 26,
                     |    "grossAmount": 100,
                     |    "receiptDate": null,
                     |    "captureDate": null,
                     |    "typeDescription": "Van Benefit",
                     |    "netAmount": null,
                     |    "employmentSequenceNumber": 1,
                     |    "costAmount": null,
                     |    "amountMadeGood": null,
                     |    "cashEquivalent": null,
                     |    "expensesIncurred": null,
                     |    "amountOfRelief": null
                     |  }]""".stripMargin






  lazy val iabdsJsonResponse = loadFile("/json/nps/response/iabds.json")
  lazy val iabdList = iabdsJsonResponse.as[List[Iabd]]
  lazy val onlyIabdList = Json.parse(onlyIabdJson).as[List[Iabd]]

  "Employment Service Helper" should {
    "correctly convert an iabd to an allowance model" in {

      val allowances =  TestEmploymentService.getAllowances(iabdList)
      allowances mustBe a [List[Allowance]]
      allowances.size mustBe 1
    }
    "Return an empty list of allowances when only iabd is present" in {
      val allowances =  TestEmploymentService.getAllowances(onlyIabdList)
      allowances mustBe a [List[Allowance]]
      allowances.size mustBe 0
    }
  }
}
