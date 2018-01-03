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

package uk.gov.hmrc.taxhistory.model.nps

import org.joda.time.LocalDate
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class GetTaxAccountSpec extends TestUtil with UnitSpec {

  lazy val getTaxAcoountResponseURLDummy = loadFile("/json/nps/response/GetTaxAccount.json")

  "GetTaxAccount" should {

    val rtiDetails = getTaxAcoountResponseURLDummy.as[RtiData](NpsTaxAccount.reader)

    "transform Rti Response Json correctly to RtiData Model " in {

      rtiDetails shouldBe a[RtiData]
      rtiDetails.nino shouldBe "AA000000"

    }
    "transform Rti Response Json correctly containing Employments" in {

      val employment49 = rtiDetails.employments.find(emp => emp.sequenceNo == 49 )
      employment49.isDefined shouldBe true

      employment49.get.sequenceNo shouldBe 49
      employment49.get.currentPayId shouldBe Some("6044041000000")
      employment49.get.officeNumber shouldBe "531"
      employment49.get.payments.size shouldBe 5
      employment49.get.earlierYearUpdates.size shouldBe 2
      employment49.get.earlierYearUpdates.head.taxablePayDelta shouldBe -600.99
      employment49.get.earlierYearUpdates.head.totalTaxDelta shouldBe -10.99
      employment49.get.earlierYearUpdates.head.receivedDate shouldBe LocalDate.parse("2016-06-01")

      val employment39 = rtiDetails.employments.find(emp => emp.sequenceNo == 39 )
      employment39.isDefined shouldBe true
      employment39.get.currentPayId shouldBe Some("111111")
      employment39.get.officeNumber shouldBe "267"
      employment39.get.payments.size shouldBe 7
      employment39.get.earlierYearUpdates.size shouldBe 0

    }
    "transform Rti Response Json correctly which containing Payments" in {
      val payments20160313 = rtiDetails.employments.map(emp => emp.payments.find(pay => pay.paidOnDate == new LocalDate(2016,3,31))).flatten
      payments20160313.size shouldBe 1
      payments20160313.head.paidOnDate shouldBe new LocalDate(2016,3,31)
      payments20160313.head.taxablePayYTD shouldBe BigDecimal.valueOf(20000.00)
      payments20160313.head.totalTaxYTD shouldBe BigDecimal.valueOf(1880.00)
    }
    "sort payment list by paid on date with latest payment in last position" in {
      val paymentsList = rtiDetails.employments.head.payments.sorted
      paymentsList.size shouldBe 5
      paymentsList.last.paidOnDate shouldBe new LocalDate(2016,3,31)
      paymentsList.last.taxablePayYTD shouldBe BigDecimal.valueOf(20000.00)
      paymentsList.last.totalTaxYTD shouldBe BigDecimal.valueOf(1880.00)
    }

    "transform Rti Response Json correctly which containing EndOfYearUpdates" in {
      val earlierYearUpdates = rtiDetails.employments.map(emp => emp.earlierYearUpdates.find(eyu => eyu.receivedDate == new LocalDate(2016,6,1))).flatten
      earlierYearUpdates.size shouldBe 1
      earlierYearUpdates.head.receivedDate shouldBe new LocalDate(2016,6,1)
      earlierYearUpdates.head.taxablePayDelta shouldBe BigDecimal.valueOf(-600.99)
      earlierYearUpdates.head.totalTaxDelta shouldBe BigDecimal.valueOf(-10.99)
    }

  }
}

