/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.model.api

import java.time.LocalDate
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, _}
import uk.gov.hmrc.taxhistory.model.nps._
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}

import java.util.UUID

class PayAsYouEarnSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues with DateUtils {

  private lazy val fullPayeJson: JsObject = loadFile("/json/model/api/paye.json").as[JsObject]

  private val employment1Id                   = "01318d7c-bcd9-47e2-8c38-551e7ccdfae3"
  private val incomeSource1DeserialisedEmpTDN = 126
  private val taAllowanceType                 = 11
  private val taDeductionType1                = 7
  private val taDeductionType2                = 8
  private val taDeductionType3                = 13

  "PayAsYouEarn" when {
    "(de)serialising all the PayAsYouEarn details" should {
      "serialise everything to a json object" in {
        toJson(PayAsYouEarn()) shouldBe a[JsObject]
      }
      "deserialise from a json object to PayAsYouEarn" in {
        fromJson[PayAsYouEarn](fullPayeJson).get shouldBe a[PayAsYouEarn]
      }
    }

    "(de)serialising the 'employments' field" should {
      val employment1      = Employment(
        employmentId = UUID.fromString(employment1Id),
        startDate = Some(LocalDate.of(YEAR_2016, JANUARY, DAY_21)),
        endDate = Some(LocalDate.of(YEAR_2017, JANUARY, DAY_1)),
        payeReference = "paye-1",
        employerName = "employer-1",
        employmentStatus = EmploymentStatus.Live,
        employmentPaymentType = Some(EmploymentPaymentType.IncapacityBenefit),
        worksNumber = "00191048716"
      )
      val employments1Json = Json.parse(s"""
           |    {
           |      "employmentId": "$employment1Id",
           |      "startDate": "2016-01-21",
           |      "endDate": "2017-01-01",
           |      "payeReference": "paye-1",
           |      "employerName": "employer-1",
           |      "employmentPaymentType": "IncapacityBenefit",
           |      "employmentStatus": 1,
           |      "worksNumber": "00191048716"
           |    }
        """.stripMargin)

      val employment2      = Employment(
        employmentId = UUID.fromString("019f5fee-d5e4-4f3e-9569-139b8ad81a87"),
        startDate = Some(LocalDate.of(YEAR_2016, FEBRUARY, DAY_22)),
        endDate = None,
        payeReference = "paye-2",
        employerName = "employer-2",
        employmentStatus = EmploymentStatus.Live,
        employmentPaymentType = None,
        worksNumber = "00191048716"
      )
      val employments2Json = Json.parse("""
          |    {
          |      "employmentId": "019f5fee-d5e4-4f3e-9569-139b8ad81a87",
          |      "startDate": "2016-02-22",
          |      "payeReference": "paye-2",
          |      "employerName": "employer-2",
          |      "employmentStatus": 1,
          |      "worksNumber": "00191048716"
          |    }
        """.stripMargin)

      val employment3      = Employment(
        employmentId = UUID.fromString(employment1Id),
        startDate = Some(LocalDate.of(YEAR_2016, JANUARY, DAY_21)),
        endDate = Some(LocalDate.of(YEAR_2017, JANUARY, DAY_1)),
        payeReference = "paye-1",
        employerName = "employer-1",
        employmentStatus = EmploymentStatus.Live,
        employmentPaymentType = Some(EmploymentPaymentType.JobseekersAllowance),
        worksNumber = "00191048716"
      )
      val employments3Json = Json.parse(s"""
           |    {
           |      "employmentId": "$employment1Id",
           |      "startDate": "2016-01-21",
           |      "endDate": "2017-01-01",
           |      "payeReference": "paye-1",
           |      "employerName": "employer-1",
           |      "employmentPaymentType": "JobseekersAllowance",
           |      "employmentStatus": 1,
           |      "worksNumber": "00191048716"
           |    }
        """.stripMargin)

      val employment4      = Employment(
        employmentId = UUID.fromString(employment1Id),
        startDate = Some(LocalDate.of(YEAR_2016, JANUARY, DAY_21)),
        endDate = Some(LocalDate.of(YEAR_2017, JANUARY, DAY_1)),
        payeReference = "paye-1",
        employerName = "employer-1",
        employmentStatus = EmploymentStatus.Live,
        employmentPaymentType = Some(EmploymentPaymentType.EmploymentAndSupportAllowance),
        worksNumber = "00191048716"
      )
      val employments4Json = Json.parse(s"""
           |    {
           |      "employmentId": "$employment1Id",
           |      "startDate": "2016-01-21",
           |      "endDate": "2017-01-01",
           |      "payeReference": "paye-1",
           |      "employerName": "employer-1",
           |      "employmentPaymentType": "EmploymentAndSupportAllowance",
           |      "employmentStatus": 1,
           |      "worksNumber": "00191048716"
           |    }
        """.stripMargin)

      val employment5      = Employment(
        employmentId = UUID.fromString(employment1Id),
        startDate = Some(LocalDate.of(YEAR_2016, JANUARY, DAY_21)),
        endDate = Some(LocalDate.of(YEAR_2017, JANUARY, DAY_1)),
        payeReference = "paye-1",
        employerName = "employer-1",
        employmentStatus = EmploymentStatus.Live,
        employmentPaymentType = Some(EmploymentPaymentType.StatePensionLumpSum),
        worksNumber = "00191048716"
      )
      val employments5Json = Json.parse(s"""
           |    {
           |      "employmentId": "$employment1Id",
           |      "startDate": "2016-01-21",
           |      "endDate": "2017-01-01",
           |      "payeReference": "paye-1",
           |      "employerName": "employer-1",
           |      "employmentPaymentType": "StatePensionLumpSum",
           |      "employmentStatus": 1,
           |      "worksNumber": "00191048716"
           |    }
        """.stripMargin)

      val employmentsDeserialised = List(employment1, employment2, employment3, employment4, employment5)
      val employmentsSerialised   =
        JsArray(Seq(employments1Json, employments2Json, employments3Json, employments4Json, employments5Json))

      "serialise to an empty json array when there are no employments" in {
        val payeNoEmployments = PayAsYouEarn(employments = Nil)
        (toJson(payeNoEmployments) \ "employments") shouldBe JsDefined(JsArray(Nil))
      }
      "serialise to a json array of employments when there are employments" in {
        val payeWithEmployments = PayAsYouEarn(employments = employmentsDeserialised)
        (toJson(payeWithEmployments) \ "employments") shouldBe JsDefined(employmentsSerialised)
      }
      "deserialise from an empty json array" in {
        val noEmploymentsJson = fullPayeJson + ("employments" -> JsArray())
        fromJson[PayAsYouEarn](noEmploymentsJson).get.employments shouldBe empty
      }
      "deserialise from a non-empty json array of employments" in {
        val withEmploymentsJson = fullPayeJson + ("employments" -> employmentsSerialised)
        fromJson[PayAsYouEarn](withEmploymentsJson).get.employments shouldBe employmentsDeserialised
      }
    }

    "(de)serialising the 'allowances' field" should {
      val allowancesDeserialised = List(
        Allowance(
          allowanceId = UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),
          iabdType = "payeType",
          amount = BigDecimal("12.00")
        )
      )
      val allowancesSerialised   = Json.parse("""
          |  [
          |    {
          |      "allowanceId": "c9923a63-4208-4e03-926d-7c7c88adc7ee",
          |      "iabdType": "payeType",
          |      "amount": 12
          |    }
          |  ]
        """.stripMargin)

      "serialise to an empty json array value when there are no allowances" in {
        val payeNoAllowances = PayAsYouEarn(allowances = Nil)
        (toJson(payeNoAllowances) \ "allowances") shouldBe JsDefined(JsArray(Nil))
      }
      "serialise to a json array of allowance objects when there are allowances" in {
        val payeWithAllowances = PayAsYouEarn(allowances = allowancesDeserialised)
        (toJson(payeWithAllowances) \ "allowances") shouldBe JsDefined(allowancesSerialised)
      }
      "deserialise from an empty json array" in {
        val noAllowancesJson = fullPayeJson + ("allowances" -> JsArray())
        fromJson[PayAsYouEarn](noAllowancesJson).get.allowances shouldBe empty
      }
      "deserialise from a non-empty json array of allowances" in {
        val withAllowancesJson = fullPayeJson + ("allowances" -> allowancesSerialised)
        fromJson[PayAsYouEarn](withAllowancesJson).get.allowances shouldBe allowancesDeserialised
      }
    }

    "(de)serialising the 'benefits' field" should {
      val benefit1             = CompanyBenefit(
        companyBenefitId = UUID.fromString("c9923a63-4208-4e03-926d-7c7c88adc7ee"),
        iabdType = "companyBenefitType",
        amount = BigDecimal("12.00"),
        source = None
      )
      val benefitsDeserialised = Map(employment1Id -> List(benefit1))
      val benefitsSerialised   = Json.parse(s"""
           |  {
           |    "$employment1Id": [
           |      {
           |        "companyBenefitId": "c9923a63-4208-4e03-926d-7c7c88adc7ee",
           |        "iabdType": "companyBenefitType",
           |        "amount": 12,
           |        "isForecastBenefit": true
           |      }
           |    ]
           |  }
        """.stripMargin)

      "serialise to an empty json object value when there are no company benefits for any employment" in {
        val payeNoBenefits = PayAsYouEarn(benefits = Map.empty)
        (toJson(payeNoBenefits) \ "benefits") shouldBe JsDefined(JsObject(Nil))
      }
      "serialise to a json object (mapping employmentId to arrays of benefits) when there are company benefits for some employments" in {
        val payeWithBenefits = PayAsYouEarn(benefits = benefitsDeserialised)
        (toJson(payeWithBenefits) \ "benefits") shouldBe JsDefined(benefitsSerialised)
      }
      "deserialise from an empty json object" in {
        val noBenefits = fullPayeJson + ("benefits" -> JsObject(Nil))
        fromJson[PayAsYouEarn](noBenefits).get.benefits shouldBe empty
      }
      "deserialise from a non-empty json object containing mappings of employmentId to arrays of company benefit objects" in {
        val withBenefitsJson = fullPayeJson + ("benefits" -> benefitsSerialised)
        fromJson[PayAsYouEarn](withBenefitsJson).get.benefits shouldBe benefitsDeserialised
      }
    }

    "(de)serialising the 'payAndTax' field" should {
      val payAndTax1            = PayAndTax(
        payAndTaxId = UUID.fromString("2e2abe0a-8c4f-49fc-bdd2-cc13054e7172"),
        taxablePayTotal = Some(BigDecimal("2222.22")),
        taxablePayTotalIncludingEYU = Some(BigDecimal("2222.23")),
        taxTotal = Some(BigDecimal("111.11")),
        taxTotalIncludingEYU = Some(BigDecimal("111.12")),
        paymentDate = Some(LocalDate.of(YEAR_2016, FEBRUARY, DAY_20)),
        studentLoan = Some(BigDecimal("333.33")),
        earlierYearUpdates = Nil
      )
      val payAndTaxDeserialised = Map(employment1Id -> payAndTax1)
      val payAndTaxSerialised   = Json.parse(s"""
           |  {
           |    "$employment1Id": {
           |      "payAndTaxId": "2e2abe0a-8c4f-49fc-bdd2-cc13054e7172",
           |      "taxablePayTotal": 2222.22,
           |      "taxablePayTotalIncludingEYU": 2222.23,
           |      "taxTotal": 111.11,
           |      "taxTotalIncludingEYU": 111.12,
           |      "paymentDate": "2016-02-20",
           |      "studentLoan": 333.33,
           |      "earlierYearUpdates": []
           |    }
           |  }
        """.stripMargin)

      "serialise to an empty json object value when there is no payAndTax for any employment" in {
        val payeNoPayAndTax = PayAsYouEarn(payAndTax = Map.empty)
        (toJson(payeNoPayAndTax) \ "payAndTax") shouldBe JsDefined(JsObject(Nil))
      }
      "serialise to a json object (mapping employmentId to payAndTax object) when there is payAndTax for some employments" in {
        val payeWithPayAndTax = PayAsYouEarn(payAndTax = payAndTaxDeserialised)
        (toJson(payeWithPayAndTax) \ "payAndTax") shouldBe JsDefined(payAndTaxSerialised)
      }
      "deserialise from an empty json object" in {
        val noPayAndTax = fullPayeJson + ("payAndTax" -> JsObject(Nil))
        fromJson[PayAsYouEarn](noPayAndTax).get.payAndTax shouldBe empty
      }
      "deserialise from a non-empty json object containing mappings of employmentId to payAndTax objects" in {
        val withPayAndTaxJson = fullPayeJson + ("payAndTax" -> payAndTaxSerialised)
        fromJson[PayAsYouEarn](withPayAndTaxJson).get.payAndTax shouldBe payAndTaxDeserialised
      }
    }

    "(de)serialising the 'incomeSources' field" should {
      val incomeSource1             = IncomeSource(
        employmentId = 1,
        employmentType = 1,
        actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("0")),
        taxCode = "227L",
        basisOperation = Some(2),
        employmentTaxDistrictNumber = incomeSource1DeserialisedEmpTDN,
        employmentPayeRef = "P32",
        allowances = List(
          TaAllowance(
            `type` = taAllowanceType,
            npsDescription = "personal allowance",
            amount = BigDecimal("11000"),
            sourceAmount = Some(BigDecimal("11000"))
          )
        ),
        deductions = List(
          TaDeduction(
            `type` = taDeductionType1,
            npsDescription = "employer benefits ",
            amount = BigDecimal("65"),
            sourceAmount = Some(BigDecimal("65"))
          ),
          TaDeduction(
            `type` = taDeductionType2,
            npsDescription = "car benefit",
            amount = BigDecimal("8026"),
            sourceAmount = Some(BigDecimal("8026"))
          ),
          TaDeduction(
            `type` = taDeductionType3,
            npsDescription = "medical insurance",
            amount = BigDecimal("637"),
            sourceAmount = Some(BigDecimal("637"))
          )
        )
      )
      val incomeSourcesDeserialised = Map(employment1Id -> incomeSource1)
      val incomeSourcesSerialised   = Json.parse(s"""
           |   {
           |      "$employment1Id": {
           |        "employmentId": 1,
           |        "employmentType": 1,
           |        "actualPUPCodedInCYPlusOneTaxYear": 0,
           |        "taxCode": "227L",
           |        "basisOperation": 2,
           |        "employmentTaxDistrictNumber": 126,
           |        "employmentPayeRef": "P32",
           |        "allowances": [
           |          {
           |            "npsDescription": "personal allowance",
           |            "amount": 11000,
           |            "type": 11,
           |            "sourceAmount": 11000
           |          }
           |        ],
           |        "deductions": [
           |          {
           |            "npsDescription": "employer benefits ",
           |            "amount": 65,
           |            "type": 7,
           |            "sourceAmount": 65
           |          },
           |          {
           |            "npsDescription": "car benefit",
           |            "amount": 8026,
           |            "type": 8,
           |            "sourceAmount": 8026
           |          },
           |          {
           |            "npsDescription": "medical insurance",
           |            "amount": 637,
           |            "type": 13,
           |            "sourceAmount": 637
           |          }
           |        ]
           |      }
           |    }
    """.stripMargin)

      "serialise to an empty json object value when there are no incomeSources for any employment" in {
        val payeNoIncomeSources = PayAsYouEarn(incomeSources = Map.empty)
        (toJson(payeNoIncomeSources) \ "incomeSources") shouldBe JsDefined(JsObject(Nil))
      }
      "serialise to a json object (mapping employmentId to incomeSource objects) when there is an incomeSource for some employments" in {
        val payeWithIncomeSources = PayAsYouEarn(incomeSources = incomeSourcesDeserialised)
        (toJson(payeWithIncomeSources) \ "incomeSources") shouldBe JsDefined(incomeSourcesSerialised)
      }
      "deserialise from an empty json object" in {
        val noIncomeSources = fullPayeJson + ("incomeSources" -> JsObject(Nil))
        fromJson[PayAsYouEarn](noIncomeSources).get.incomeSources shouldBe empty
      }
      "deserialise from a non-empty json object containing mappings of employmentId to incomeSource objects" in {
        val withIncomeSourceJson = fullPayeJson + ("incomeSources" -> incomeSourcesSerialised)
        fromJson[PayAsYouEarn](withIncomeSourceJson).get.incomeSources shouldBe incomeSourcesDeserialised
      }
    }

    "(de)serialising the 'taxAccount' field" should {
      val taxAccountDeserialised = Some(
        TaxAccount(
          taxAccountId = UUID.fromString("3923afda-41ee-4226-bda5-e39cc4c82934"),
          outstandingDebtRestriction = Some(BigDecimal("22.22")),
          underpaymentAmount = Some(BigDecimal("11.11")),
          actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("33.33"))
        )
      )
      val taxAccountSerialised   = Json.parse("""
          |  {
          |    "taxAccountId": "3923afda-41ee-4226-bda5-e39cc4c82934",
          |    "outstandingDebtRestriction": 22.22,
          |    "underpaymentAmount": 11.11,
          |    "actualPUPCodedInCYPlusOneTaxYear": 33.33
          |  }
        """.stripMargin)

      "serialise to a missing field when the optional TaxAccount is empty" in {
        val payeNoTaxAccount = PayAsYouEarn(taxAccount = None)
        (toJson(payeNoTaxAccount) \ "taxAccount") shouldBe a[JsUndefined]
      }
      "serialise to a json object when there is some TaxAccount details present" in {
        val payeWithTaxAccount = PayAsYouEarn(taxAccount = taxAccountDeserialised)
        (toJson(payeWithTaxAccount) \ "taxAccount") shouldBe JsDefined(taxAccountSerialised)
      }
      "deserialise from a missing field to a None" in {
        val noTaxAccount = fullPayeJson - "taxAccount"
        fromJson[PayAsYouEarn](noTaxAccount).get.taxAccount shouldBe None
      }
      "deserialise from a non-empty json object to some TaxAccount" in {
        val withTaxAccountJson = fullPayeJson + ("taxAccount" -> taxAccountSerialised)
        fromJson[PayAsYouEarn](withTaxAccountJson).get.taxAccount shouldBe taxAccountDeserialised
      }
    }

    "(de)serialising the 'statePension' field" should {
      val statePensionDeserialised = Some(
        StatePension(
          grossAmount = BigDecimal("1253"),
          typeDescription = "State Pension",
          paymentFrequency = Some(1),
          startDate = Some(LocalDate.parse("2018-01-23"))
        )
      )
      val statePensionSerialised   = Json.parse("""
          |{
          |    "grossAmount": 1253,
          |    "typeDescription": "State Pension",
          |    "paymentFrequency": 1,
          |    "startDate": "2018-01-23"
          |}
        """.stripMargin)

      "serialise to a missing field when the optional StatePension is empty" in {
        val payeNoStatePension = PayAsYouEarn(statePension = None)
        (toJson(payeNoStatePension) \ "statePension") shouldBe a[JsUndefined]
      }
      "serialise to a json object when there is some StatePension details present" in {
        val payeWithStatePension = PayAsYouEarn(statePension = statePensionDeserialised)
        (toJson(payeWithStatePension) \ "statePension") shouldBe JsDefined(statePensionSerialised)
      }
      "deserialise from a missing field to a None" in {
        val noStatePension = fullPayeJson - "statePension"
        fromJson[PayAsYouEarn](noStatePension).get.statePension shouldBe None
      }
      "deserialise from a non-empty json object to some StatePension" in {
        val withStatePensionJson = fullPayeJson + ("statePension" -> statePensionSerialised)
        fromJson[PayAsYouEarn](withStatePensionJson).get.statePension shouldBe statePensionDeserialised
      }
    }
  }
}
