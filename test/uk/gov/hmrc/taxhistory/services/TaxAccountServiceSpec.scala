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
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.taxhistory.model.api.{PayAsYouEarn, TaxAccount}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.nps.{NpsEmployment, NpsTaxAccount}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.utils.TestEmploymentHistoryService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class TaxAccountServiceSpec extends PlaySpec with MockitoSugar with TestUtil {

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()

  val testEmploymentHistoryService = TestEmploymentHistoryService.createNew()

  val failureResponseJson = Json.parse("""{"reason":"Bad Request"}""")


  val npsEmploymentResponse :List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), false, false,
      new LocalDate("2015-01-21"), None, true, Live))



  lazy val taxAccountResponseJson = loadFile("/json/nps/response/GetTaxAccount.json")
  lazy val taxAccountResponse = taxAccountResponseJson.as[NpsTaxAccount]

  "TaxAccount" should {
    "successfully be populated from GetTaxAccount" in {
      when(testEmploymentHistoryService.npsConnector.getEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(npsEmploymentResponse))
      when(testEmploymentHistoryService.npsConnector.getIabds(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new NotFoundException("")))
      when(testEmploymentHistoryService.npsConnector.getTaxAccount(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.successful(taxAccountResponse))
      when(testEmploymentHistoryService.rtiConnector.getRTIEmployments(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]))
        .thenReturn(Future.failed(new NotFoundException("")))

      val payAsYouEarn = await(testEmploymentHistoryService.retrieveEmploymentsDirectFromSource(testNino, TaxYear(2016)))

      val taxAccount = payAsYouEarn.taxAccount.get
      taxAccount.outstandingDebtRestriction mustBe Some(145.75)
      taxAccount.underpaymentAmount mustBe Some(15423.29)
      taxAccount.actualPUPCodedInCYPlusOneTaxYear mustBe Some(240)
    }

    "successfully retrieve tax account from cache" in {
      lazy val paye = loadFile("/json/model/api/paye.json").as[PayAsYouEarn]
      val testTaxAccount = TaxAccount(UUID.fromString("3923afda-41ee-4226-bda5-e39cc4c82934"), Some(22.22), Some(11.11),Some(33.33))

      when(testEmploymentHistoryService.getFromCache(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(paye))

      val taxAccount = await(testEmploymentHistoryService.getTaxAccount(testNino, TaxYear.current.previous))
      taxAccount must be(testTaxAccount)
    }
  }
}
