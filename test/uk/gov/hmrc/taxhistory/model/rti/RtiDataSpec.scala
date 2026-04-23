/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.model.rti

import java.time.LocalDate
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json.fromJson
import play.api.libs.json.{JsArray, JsObject, JsSuccess, JsValue, Json}
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}

class RtiDataSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues with DateUtils {

  lazy val rtiSuccessfulResponseURLDummy: JsValue = loadFile("/json/rti/response/dummyRti.json")

  "RtiData" should {

    val rtiDetails = rtiSuccessfulResponseURLDummy.as[RtiData](using RtiData.reader)

    val rtiJson = Json.parse(s"""
         |  {
         |  "nino":"AA000000",
         |  "employments":[
         |    {
         |    "sequenceNo":49,
         |    "officeNumber":"531",
         |    "payeRef":"J4816",
         |    "currentPayId":"6044041000000",
         |    "payments":[
         |        {
         |          "paidOnDate":"2016-03-31",
         |          "taxablePayYTD":20000,
         |          "totalTaxYTD":1880,
         |          "studentLoansYTD":333.33
         |          },
         |        {
         |          "paidOnDate":"2016-02-29",
         |          "taxablePayYTD":18000,
         |          "totalTaxYTD":1680,
         |          "studentLoansYTD":0
         |          },
         |        {
         |          "paidOnDate":"2016-01-29",
         |          "taxablePayYTD":16000,
         |          "totalTaxYTD":1480,
         |          "studentLoansYTD":0
         |          },
         |        {
         |          "paidOnDate":"2015-12-31",
         |          "taxablePayYTD":12000,
         |          "totalTaxYTD":1080,
         |          "studentLoansYTD":0
         |          },
         |        {
         |          "paidOnDate":"2015-11-30",
         |          "taxablePayYTD":8000,
         |          "totalTaxYTD":880,
         |          "studentLoansYTD":0
         |          }
         |        ]
         |    },
         |    {
         |      "sequenceNo":39,
         |      "officeNumber":"267",
         |      "payeRef":"G697",
         |      "currentPayId":"111111",
         |      "payments":[
         |         {
         |          "paidOnDate":"2015-04-30",
         |          "taxablePayYTD":20000,
         |          "totalTaxYTD":1880,
         |          "studentLoansYTD":0
         |          },
         |          {
         |          "paidOnDate":"2015-05-29",
         |          "taxablePayYTD":20000,
         |          "totalTaxYTD":1880,
         |          "studentLoansYTD":0
         |          },
         |          {
         |          "paidOnDate":"2015-06-30",
         |          "taxablePayYTD":20000,
         |          "totalTaxYTD":1880,
         |          "studentLoansYTD":0
         |          },
         |          {
         |          "paidOnDate":"2016-02-28",
         |          "taxablePayYTD":19000,
         |          "totalTaxYTD":5250
         |          },
         |          {
         |          "paidOnDate":"2015-07-31",
         |          "taxablePayYTD":11000,
         |          "totalTaxYTD":3250
         |          },
         |          {"paidOnDate":"2015-04-30",
         |          "taxablePayYTD":5000,
         |          "totalTaxYTD":1500
         |          },
         |          {
         |          "paidOnDate":"2015-10-31",
         |          "taxablePayYTD":15000,
         |          "totalTaxYTD":4250
         |          }
         |          ]
         |          }
         |          ]
         |          }
         | """.stripMargin)

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(rtiDetails) shouldBe rtiJson
      }
    }

    "transform Rti Response Json correctly to RtiData Model " in {
      rtiDetails      shouldBe a[RtiData]
      rtiDetails.nino shouldBe "AA000000"
    }

    "transform Rti Response Json correctly containing Employments" in {
      val employment49 = rtiDetails.employments.find(emp => emp.sequenceNo == 49)
      employment49.isDefined shouldBe true

      employment49.get.sequenceNo                  shouldBe 49
      employment49.get.currentPayId                shouldBe Some("6044041000000")
      employment49.get.officeNumber                shouldBe "531"
      employment49.get.payments.size               shouldBe 5
      employment49.get.toPayAndTax.studentLoan.get shouldBe BigDecimal.valueOf(333.33)

      val employment39 = rtiDetails.employments.find(emp => emp.sequenceNo == 39)
      employment39.isDefined                   shouldBe true
      employment39.get.currentPayId            shouldBe Some("111111")
      employment39.get.officeNumber            shouldBe "267"
      employment39.get.payments.size           shouldBe 7
      employment39.get.toPayAndTax.studentLoan shouldBe None
    }

    "transform Rti Response Json correctly which containing Payments" in {
      val payments20160313 =
        rtiDetails.employments.flatMap(emp =>
          emp.payments.find(pay => pay.paidOnDate == LocalDate.of(YEAR_2016, MARCH, DAY_31))
        )
      payments20160313.size                     shouldBe 1
      payments20160313.head.paidOnDate          shouldBe LocalDate.of(YEAR_2016, MARCH, DAY_31)
      payments20160313.head.taxablePayYTD       shouldBe BigDecimal.valueOf(20000.00)
      payments20160313.head.totalTaxYTD         shouldBe BigDecimal.valueOf(1880.00)
      payments20160313.head.studentLoansYTD.get shouldBe BigDecimal.valueOf(333.33)
    }

    "sort payment list by paid on date with latest payment in last position" in {
      val paymentsList = rtiDetails.employments.head.payments.sorted
      paymentsList.size               shouldBe 5
      paymentsList.last.paidOnDate    shouldBe LocalDate.of(YEAR_2016, MARCH, DAY_31)
      paymentsList.last.taxablePayYTD shouldBe BigDecimal.valueOf(20000.00)
      paymentsList.last.totalTaxYTD   shouldBe BigDecimal.valueOf(1880.00)
    }

    "transform Rti Response Json containing inYear payment but no eyu payment" in {
      val rtiResponse: JsValue = loadFile("/json/rti/response/dummyRtiHasOnlyInYearPayments.json")
      val rtiDetails           = rtiResponse.as[RtiData](using RtiData.reader)
      val employment           = rtiDetails.employments.head
      employment.payments.size shouldBe 5
    }
  }

  "RtiPayment" should {

    val validRtiPayment = RtiPayment(
      paidOnDate = LocalDate.parse("2014-04-28"),
      taxablePayYTD = BigDecimal("20000.00"),
      totalTaxYTD = BigDecimal("1880.00"),
      studentLoansYTD = Some(BigDecimal("333.33"))
    )

    val rtiPaymentJson = Json.obj(
      "paidOnDate"      -> "2014-04-28",
      "taxablePayYTD"   -> BigDecimal(20000),
      "totalTaxYTD"     -> BigDecimal(1880),
      "studentLoansYTD" -> 333.33
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(validRtiPayment) shouldBe rtiPaymentJson
      }

      "an optional field is missing" in {
        Json.toJson(validRtiPayment.copy(studentLoansYTD = None)) shouldBe Json.obj(
          "paidOnDate"    -> "2014-04-28",
          "taxablePayYTD" -> BigDecimal(20000),
          "totalTaxYTD"   -> BigDecimal(1880)
        )
      }
    }

    "use default studentLoansYTD of None when not specified" in {
      val payment = RtiPayment(
        paidOnDate = LocalDate.parse("2014-04-28"),
        taxablePayYTD = BigDecimal("100.00"),
        totalTaxYTD = BigDecimal("10.00")
      )
      payment.studentLoansYTD shouldBe None
    }

    "deserialize from JSON" should {

      "when all fields are present" in {
        Json
          .obj(
            "pmtDate"         -> "2014-04-28",
            "taxablePayYTD"   -> BigDecimal(20000),
            "totalTaxYTD"     -> BigDecimal(1880),
            "studentLoansYTD" -> 333.33
          )
          .validate[RtiPayment] shouldEqual JsSuccess(
          RtiPayment(
            paidOnDate = LocalDate.parse("2014-04-28"),
            taxablePayYTD = BigDecimal(0),
            totalTaxYTD = BigDecimal(0),
            studentLoansYTD = None
          )
        )
      }
    }
  }
}
