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
import play.api.libs.json.{JsArray, JsError, JsObject, JsSuccess, JsValue, Json}
import uk.gov.hmrc.taxhistory.model.api.EarlierYearUpdate
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}

import java.util.UUID

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
         |        ],
         |     "earlierYearUpdates":[
         |      {
         |        "taxablePayDelta":-600.99,
         |        "totalTaxDelta":-10.99,
         |        "receivedDate":"2016-06-01"
         |        },
         |        {
         |        "taxablePayDelta":0,
         |        "totalTaxDelta":0,
         |        "receivedDate":"2016-06-01"
         |        }
         |      ]
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
         |          ],
         |          "earlierYearUpdates":[]
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

      employment49.get.sequenceNo                              shouldBe 49
      employment49.get.currentPayId                            shouldBe Some("6044041000000")
      employment49.get.officeNumber                            shouldBe "531"
      employment49.get.payments.size                           shouldBe 5
      employment49.get.earlierYearUpdates.size                 shouldBe 2
      employment49.get.earlierYearUpdates.head.taxablePayDelta shouldBe -600.99
      employment49.get.earlierYearUpdates.head.totalTaxDelta   shouldBe -10.99
      employment49.get.earlierYearUpdates.head.receivedDate    shouldBe LocalDate.parse("2016-06-01")
      employment49.get.toPayAndTax.studentLoan.get             shouldBe BigDecimal.valueOf(333.33)

      val employment39 = rtiDetails.employments.find(emp => emp.sequenceNo == 39)
      employment39.isDefined                   shouldBe true
      employment39.get.currentPayId            shouldBe Some("111111")
      employment39.get.officeNumber            shouldBe "267"
      employment39.get.payments.size           shouldBe 7
      employment39.get.earlierYearUpdates.size shouldBe 0
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

    "transform Rti Response Json correctly which containing EndOfYearUpdates" in {
      val earlierYearUpdates = rtiDetails.employments.flatMap(emp =>
        emp.earlierYearUpdates.find(eyu => eyu.receivedDate == LocalDate.of(YEAR_2016, JUNE, DAY_1))
      )
      earlierYearUpdates.size                 shouldBe 1
      earlierYearUpdates.head.receivedDate    shouldBe LocalDate.of(YEAR_2016, JUNE, DAY_1)
      earlierYearUpdates.head.taxablePayDelta shouldBe BigDecimal.valueOf(-600.99)
      earlierYearUpdates.head.totalTaxDelta   shouldBe BigDecimal.valueOf(-10.99)
    }

    "transform Rti Response Json containing inYear payment but no eyu payment" in {
      val rtiResponse: JsValue = loadFile("/json/rti/response/dummyRtiHasOnlyInYearPayments.json")
      val rtiDetails           = rtiResponse.as[RtiData](using RtiData.reader)
      val employment           = rtiDetails.employments.head

      employment.earlierYearUpdates shouldBe List.empty
      employment.payments.size      shouldBe 5
    }

    "transform Rti Response Json containing eyu payment but no inYear payment" in {
      val rtiResponse: JsValue = loadFile("/json/rti/response/dummyRtiHasOnlyEyuPayments.json")
      val rtiDetails           = rtiResponse.as[RtiData](using RtiData.reader)
      val employment           = rtiDetails.employments.head

      employment.earlierYearUpdates.size shouldBe 2
      employment.payments                shouldBe List.empty
    }
  }

  "RtiEarlierYearUpdate" when {
    val eyuSerialised: JsObject = Json
      .parse(s"""
         |{
         |  "optionalAdjustmentAmount": [
         |    {
         |      "type": "TaxablePayDelta",
         |      "amount": -600.99
         |    },
         |    {
         |      "type": "TotalTaxDelta",
         |      "amount": -10.99
         |    },
         |    {
         |      "type": "StudentLoanRecoveredDelta",
         |      "amount": 333.33
         |    }
         |  ],
         |  "rcvdDate": "2016-06-01"
         |}
        """.stripMargin)
      .as[JsObject]

    val eyuDeserialised = RtiEarlierYearUpdate(
      taxablePayDelta = BigDecimal(-600.99),
      totalTaxDelta = BigDecimal(-10.99),
      studentLoanRecoveredDelta = Some(333.33),
      receivedDate = LocalDate.parse("2016-06-01")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(eyuDeserialised) shouldBe Json.obj(
          "taxablePayDelta"           -> BigDecimal(-600.99),
          "totalTaxDelta"             -> BigDecimal(-10.99),
          "studentLoanRecoveredDelta" -> BigDecimal(333.33),
          "receivedDate"              -> "2016-06-01"
        )
      }

      "an optional field is missing" in {
        Json.toJson(eyuDeserialised.copy(studentLoanRecoveredDelta = None)) shouldBe Json.obj(
          "taxablePayDelta" -> BigDecimal(-600.99),
          "totalTaxDelta"   -> BigDecimal(-10.99),
          "receivedDate"    -> "2016-06-01"
        )
      }
    }

    "deserialising from an RTI EYU json object" should {
      "deserialise to an RtiEarlierYearUpdate" when {
        "the EYU's 'optionalAdjustmentAmount' array is present" in {
          fromJson[RtiEarlierYearUpdate](eyuSerialised).get shouldBe eyuDeserialised
        }
        "the EYU's 'optionalAdjustmentAmount' array is not present" in {
          val eyuWithoutAdjustmentAmount = eyuSerialised - "optionalAdjustmentAmount"
          fromJson[RtiEarlierYearUpdate](eyuWithoutAdjustmentAmount).get shouldBe RtiEarlierYearUpdate(
            taxablePayDelta = BigDecimal("0"),
            totalTaxDelta = BigDecimal("0"),
            studentLoanRecoveredDelta = None,
            receivedDate = eyuDeserialised.receivedDate
          )
        }
      }

      "deserialise the taxablePayDelta field" when {
        "the EYU's 'optionalAdjustmentAmount' contains an object with 'type' of 'TaxablePayDelta'" in {
          fromJson[RtiEarlierYearUpdate](eyuSerialised).get.taxablePayDelta shouldBe BigDecimal("-600.99")
        }
        "the EYU's 'optionalAdjustmentAmount' does not contain an object with 'type' of 'TaxablePayDelta'" in {
          val eyuWithoutTaxablePayDelta = eyuSerialised + ("optionalAdjustmentAmount" -> JsArray())
          fromJson[RtiEarlierYearUpdate](eyuWithoutTaxablePayDelta).get.taxablePayDelta shouldBe BigDecimal("0")
        }
      }

      "deserialise the totalTaxDelta field" when {
        "the EYU's 'optionalAdjustmentAmount' contains an object with 'type' of 'TotalTaxDelta'" in {
          fromJson[RtiEarlierYearUpdate](eyuSerialised).get.totalTaxDelta shouldBe BigDecimal("-10.99")
        }
        "the EYU's 'optionalAdjustmentAmount' does not contain an object with 'type' of 'TotalTaxDelta'" in {
          val eyuWithoutTotalTaxDelta = eyuSerialised + ("optionalAdjustmentAmount" -> JsArray())
          fromJson[RtiEarlierYearUpdate](eyuWithoutTotalTaxDelta).get.totalTaxDelta shouldBe BigDecimal("0")
        }
      }

      "deserialise the studentLoanRecoveredDelta field" when {
        "the EYU's 'optionalAdjustmentAmount' contains an object with 'type' of 'StudentLoanRecoveredDelta'" in {
          fromJson[RtiEarlierYearUpdate](eyuSerialised).get.studentLoanRecoveredDelta shouldBe Some(
            BigDecimal("333.33")
          )
        }
        "the EYU's 'optionalAdjustmentAmount' does not contain an object with 'type' of 'TotalTaxDelta'" in {
          val eyuWithoutTotalTaxDelta = eyuSerialised + ("optionalAdjustmentAmount" -> JsArray())
          fromJson[RtiEarlierYearUpdate](eyuWithoutTotalTaxDelta).get.studentLoanRecoveredDelta shouldBe None
        }
      }
    }

    "toEarlierYearUpdate is called" should {
      val testUuid = UUID.randomUUID()

      "convert itself to an EarlierYearUpdate" in {
        val taxHistoryEyu = eyuDeserialised.toEarlierYearUpdate.copy(earlierYearUpdateId = testUuid)
        taxHistoryEyu shouldBe EarlierYearUpdate(
          taxablePayEYU = eyuDeserialised.taxablePayDelta,
          taxEYU = eyuDeserialised.totalTaxDelta,
          studentLoanEYU = eyuDeserialised.studentLoanRecoveredDelta,
          receivedDate = eyuDeserialised.receivedDate,
          earlierYearUpdateId = testUuid
        )
      }

      "generate a random earlierYearUpdateId" in {
        val eyu1 = eyuDeserialised.toEarlierYearUpdate
        val eyu2 = eyuDeserialised.toEarlierYearUpdate
        eyu1 should not be eyu2

        eyu1.copy(earlierYearUpdateId = testUuid) shouldBe eyu2.copy(earlierYearUpdateId = testUuid)
      }
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
        println(Json.toJson(validRtiPayment))
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
