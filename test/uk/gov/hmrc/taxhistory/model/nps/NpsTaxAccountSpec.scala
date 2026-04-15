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

package uk.gov.hmrc.taxhistory.model.nps

import java.time.LocalDate
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json.fromJson
import play.api.libs.json.*
import uk.gov.hmrc.domain.TaxCode
import uk.gov.hmrc.taxhistory.model.api.IncomeSource
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}

class NpsTaxAccountSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues with DateUtils {

  lazy val getHipTaxAccountResponseURLDummy: JsValue = loadFile("/json/nps/response/GetTaxAccount.json")

  private val primaryEmploymentId              = 1
  private val actualPupCodedInCYPlusOne        = 99999.98
  private val outStandingDebt                  = 145.75
  private val underPayment                     = 15423.29
  private val sequenceNumberInt                = 6
  private val taDeductionType                  = 15
  private val taAllowanceType                  = 8
  private val testNpsIncomeSourceEmpId         = 6
  private val testNpsIncomeSourceEmpTDN        = 961
  private val testNpsIncomeSourceEmpType       = 23
  private val testNpsIncomeBasisOperation      = 64
  private val incomeSource1DeserialisedEmpId   = 1
  private val incomeSource1DeserialisedEmpType = 1
  private val incomeSource1DeserialisedEmpTDN  = 111

  private val taxAccount: NpsTaxAccount = getHipTaxAccountResponseURLDummy.as[NpsTaxAccount]

  private val testNpsEmployment = NpsEmployment(
    nino = "AA000000",
    sequenceNumber = sequenceNumberInt,
    taxDistrictNumber = "961",
    payeNumber = "AZ00010",
    employerName = "Aldi",
    worksNumber = Some("6044041000000"),
    startDate = Some(LocalDate.of(YEAR_2015, JANUARY, DAY_21)),
    endDate = None,
    employmentStatus = Live
  )

  private val testDeductions      = List(
    AllowanceOrDeduction(
      `type` = taDeductionType,
      npsDescription = "balancing charge",
      amount = BigDecimal("212"),
      sourceAmount = Some(212)
    )
  )
  private val testAllowances      = List(
    AllowanceOrDeduction(
      `type` = taAllowanceType,
      npsDescription = "loan interest",
      amount = BigDecimal("11500"),
      sourceAmount = Some(0)
    )
  )
  private val testNpsIncomeSource = NpsIncomeSource(
    employmentId = testNpsIncomeSourceEmpId,
    employmentTaxDistrictNumber = Some(testNpsIncomeSourceEmpTDN),
    employmentPayeRef = Some("AZ00010"),
    actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal(3.14)),
    deductions = testDeductions,
    allowances = testAllowances,
    employmentType = Some(testNpsIncomeSourceEmpType),
    taxCode = Some("TAX1234"),
    basisOperation = Some(testNpsIncomeBasisOperation)
  )

  private val testNpsIncomeSourceHip = NpsIncomeSource(
    employmentId = testNpsIncomeSourceEmpId,
    employmentTaxDistrictNumber = Some(testNpsIncomeSourceEmpTDN),
    employmentPayeRef = Some("AZ00010"),
    actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal(3.14)),
    deductions = testDeductions,
    allowances = testAllowances,
    employmentType = Some(1),
    taxCode = Some("TAX1234"),
    basisOperation = Some(2)
  )

  private val testNpsIncomeSourceHipJson = Json.obj(
    "employmentSequenceNumber" -> testNpsIncomeSourceEmpId,
    "employmentRecordType"     -> "PRIMARY",
    "actualPUPCodedInNextYear" -> BigDecimal(3.14),
    "deductionsDetails"        -> Json.arr(
      Json.obj("type" -> "balancing charge (015)", "sourceAmount" -> 212, "adjustedAmount" -> 212)
    ),
    "allowancesDetails"        -> Json.arr(
      Json.obj("type" -> "loan interest (008)", "sourceAmount" -> 0, "adjustedAmount" -> 11500)
    ),
    "taxCode"                  -> "TAX1234",
    "basisOfOperation"         -> "Cumulative",
    "employerReference"        -> s"$testNpsIncomeSourceEmpTDN/AZ00010"
  )

  "NpsTaxAccount" when {
    "transforming NPS Get Tax Account API response Json correctly to NpsTaxAccount Model " in {
      taxAccount                              shouldBe a[NpsTaxAccount]
      taxAccount.getPrimaryEmploymentId       shouldBe Some(primaryEmploymentId)
      taxAccount.getActualPupCodedInCYPlusOne shouldBe Some(actualPupCodedInCYPlusOne)
      taxAccount.getOutStandingDebt           shouldBe None
      taxAccount.getUnderPayment              shouldBe None
    }

    "deserialising the 'incomeSources' field" should {
      val incomeSource1Deserialised = NpsIncomeSource(
        employmentId = incomeSource1DeserialisedEmpId,
        employmentType = Some(incomeSource1DeserialisedEmpType),
        actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("99999.98")),
        deductions = testDeductions,
        allowances = testAllowances,
        taxCode = Some("K20"),
        basisOperation = Some(1),
        employmentTaxDistrictNumber = Some(incomeSource1DeserialisedEmpTDN),
        employmentPayeRef = Some("A11111")
      )
      val incomeSourcesDeserialised = List(incomeSource1Deserialised)

      val incomeSourcesSerialised = Json.parse(s"""
        [
          {
            "employmentSequenceNumber": 1,
            "employmentRecordType": "PRIMARY",
            "actualPUPCodedInNextYear": 99999.98,
            "deductionsDetails": [
              {"type": "balancing charge (015)", "sourceAmount": 212, "adjustedAmount": 212}
            ],
            "allowancesDetails": [
              {"type": "loan interest (008)", "sourceAmount": 0, "adjustedAmount": 11500}
            ],
            "taxCode": "K20",
            "basisOfOperation": "Week1/Month1",
            "employerReference": "111/A11111"
          }
        ]
      """.stripMargin)

      "deserialise from an empty json array" in {
        val jsonIncomeSourcesMissing =
          getHipTaxAccountResponseURLDummy.as[JsObject] + ("employmentDetailsList"        -> JsArray())
        fromJson[NpsTaxAccount](jsonIncomeSourcesMissing).get.employmentDetailsList shouldBe empty
      }

      "deserialise from a non-empty json array of allowances" in {
        val jsonIncomeSourcesPresent =
          getHipTaxAccountResponseURLDummy.as[JsObject] + ("employmentDetailsList" -> incomeSourcesSerialised)
        fromJson[NpsTaxAccount](jsonIncomeSourcesPresent).get.employmentDetailsList
          .take(1)                                                           shouldBe incomeSourcesDeserialised
      }

      "deserialise when 'employmentType' contains a null value (ASA-265)" in {
        val noEmploymentTypeJson = (incomeSourcesSerialised \ 0).as[JsObject] + ("employmentRecordType" -> JsNull)
        fromJson[NpsIncomeSource](noEmploymentTypeJson).get.employmentType shouldBe None
      }

      "deserialise when 'employmentTaxDistrictNumber' contains a null value (ASA-265)" in {
        val noEmplTaxDistrictNumJson =
          (incomeSourcesSerialised \ 0).as[JsObject] + ("employerReference"                       -> JsNull)
        fromJson[NpsIncomeSource](noEmplTaxDistrictNumJson).get.employmentTaxDistrictNumber shouldBe None
      }

      "deserialise when 'employmentPayeRef' contains a null value (ASA-265)" in {
        val noEmploymentPayeRefJson = (incomeSourcesSerialised \ 0).as[JsObject] + ("employerReference" -> JsNull)
        fromJson[NpsIncomeSource](noEmploymentPayeRefJson).get.employmentPayeRef shouldBe None
      }
      "deserialise when 'taxCode' contains a null value (ASA-265)" in {
        val noTaxCodeJson = (incomeSourcesSerialised \ 0).as[JsObject] + ("taxCode" -> JsNull)
        fromJson[NpsIncomeSource](noTaxCodeJson).get.taxCode shouldBe None
      }

      "deserialise when 'taxCode' contains a value that is not valid for the TaxCode domain class (ASA-265)" in {
        val unusualTaxCode         = "FNORDS"
        assertThrows[IllegalArgumentException] {
          TaxCode(unusualTaxCode)
        }
        val withUnusualTaxCodeJson =
          (incomeSourcesSerialised \ 0).as[JsObject] + ("taxCode" -> JsString(unusualTaxCode))
        val jsResult = fromJson[NpsIncomeSource](withUnusualTaxCodeJson)
        jsResult             shouldBe a[JsSuccess[?]]
        jsResult.get.taxCode shouldBe Some(unusualTaxCode)
      }
    }

    "matchedIncomeSource is called" should {
      "return None when no employment and income source match" in {
        val sequenceNumber = 6
        val npsEmployment  = NpsEmployment(
          "AA000000",
          sequenceNumber,
          "111",
          "AZ00010",
          "Aldi",
          Some("6044041000000"),
          receivingJobSeekersAllowance = false,
          otherIncomeSourceIndicator = false,
          Some(LocalDate.of(YEAR_2015, JANUARY, DAY_21)),
          None,
          receivingOccupationalPension = false,
          Live
        )

        taxAccount.matchedIncomeSource(npsEmployment) shouldBe None
      }

      "return a single income source when income sources match the employment" in {
        val targetEmploymentId = 1
        val npsEmployment      = NpsEmployment(
          "AA000000",
          targetEmploymentId,
          "111",
          "A11111",
          "Aldi",
          Some("6044041000000"),
          receivingJobSeekersAllowance = false,
          otherIncomeSourceIndicator = false,
          Some(LocalDate.of(YEAR_2015, JANUARY, DAY_21)),
          None,
          receivingOccupationalPension = false,
          Live
        )

        taxAccount.matchedIncomeSource(npsEmployment).get.employmentId shouldBe targetEmploymentId
      }

      "return None if the income source's would normally match the employment but the employmentTaxDistrictNumber or " +
        "employmentPayeRef is not present (ASA-265)" in {
          val targetEmploymentId     = 6
          val matchingTaxDistrictNum = 961
          val matchingEmploymentRef  = "AZ00010"
          val npsEmployment          = testNpsEmployment.copy(
            sequenceNumber = targetEmploymentId,
            taxDistrictNumber = matchingTaxDistrictNum.toString,
            payeNumber = matchingEmploymentRef
          )

          val incomeSource = testNpsIncomeSource.copy(
            employmentId = targetEmploymentId,
            employmentTaxDistrictNumber = Some(matchingTaxDistrictNum),
            employmentPayeRef = Some(matchingEmploymentRef)
          )

          NpsTaxAccount(
            List(
              incomeSource
            )
          ).matchedIncomeSource(npsEmployment).get.employmentId shouldBe targetEmploymentId

          NpsTaxAccount(
            List(
              incomeSource.copy(employmentTaxDistrictNumber = None)
            )
          ).matchedIncomeSource(npsEmployment) shouldBe None

          NpsTaxAccount(
            List(
              incomeSource.copy(employmentPayeRef = None)
            )
          ).matchedIncomeSource(npsEmployment) shouldBe None
        }

      "match employmentTaxDistrictNumber even if NpsEmployment taxDistrictNumber is zero-padded string" in {
        val incomeSource  = NpsIncomeSource(
          employmentId = 1,
          employmentType = Some(1),
          actualPUPCodedInCYPlusOneTaxYear = None,
          deductions = Nil,
          allowances = Nil,
          taxCode = Some("123L"),
          basisOperation = None,
          employmentTaxDistrictNumber = Some(80),
          employmentPayeRef = Some("AB123")
        )
        val taxAccount    = NpsTaxAccount(List(incomeSource))
        val npsEmployment = NpsEmployment(
          nino = "AA000000",
          sequenceNumber = 1,
          taxDistrictNumber = "080", // zero-padded
          payeNumber = "AB123",
          employerName = "Test",
          worksNumber = None,
          startDate = None,
          endDate = None,
          employmentStatus = EmploymentStatus.Live
        )
        taxAccount.matchedIncomeSource(npsEmployment) shouldBe defined
      }
    }

    "getPrimaryEmploymentId is called" should {
      "return the employmentId if there is an income source whose employmentType indicates a primary income " in {
        val employmentTypePrimaryIncome = 1
        NpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypePrimaryIncome)
            )
          )
        ).getPrimaryEmploymentId shouldBe Some(testNpsIncomeSource.employmentId)
      }

      "return None if there is no income source whose employmentType indicates a primary income" in {
        val employmentTypeNotPrimary = 2
        NpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypeNotPrimary)
            )
          )
        ).getPrimaryEmploymentId shouldBe None
      }

      "work with missing employmentType (ASA-265)" in {
        NpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = None
            )
          )
        ).getPrimaryEmploymentId shouldBe None
      }
    }

    "getOutStandingDebt is called" should {
      val employmentTypePrimaryIncome  = 1
      val deductionTypeOutstandingDebt = 41
      val deductionWithOutstandingDebt = AllowanceOrDeduction(
        `type` = deductionTypeOutstandingDebt,
        npsDescription = "desc",
        amount = BigDecimal("123"),
        sourceAmount = Some(BigDecimal("456"))
      )

      "return the sourceAmount from a primary income with a deduction indicating outstanding debt" in {
        NpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypePrimaryIncome),
              deductions = List(deductionWithOutstandingDebt)
            )
          )
        ).getOutStandingDebt shouldBe deductionWithOutstandingDebt.sourceAmount
      }

      "work with missing employmentType (ASA-265)" in {
        NpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = None,
              deductions = List(deductionWithOutstandingDebt)
            )
          )
        ).getOutStandingDebt shouldBe None
      }
    }

    "getUnderPayment is called" should {
      val employmentTypePrimaryIncome     = 1
      val deductionTypeUnderpaymentAmount = 35
      val deductionWithUnderpayment       = AllowanceOrDeduction(
        `type` = deductionTypeUnderpaymentAmount,
        npsDescription = "desc",
        amount = BigDecimal("123"),
        sourceAmount = Some(BigDecimal("456"))
      )

      "return the sourceAmount from a primary income with a deduction indicating an underpayment amount" in {
        NpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypePrimaryIncome),
              deductions = List(deductionWithUnderpayment)
            )
          )
        ).getUnderPayment shouldBe deductionWithUnderpayment.sourceAmount
      }

      "work with missing employmentType (ASA-265)" in {
        NpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = None,
              deductions = List(deductionWithUnderpayment)
            )
          )
        ).getUnderPayment shouldBe None
      }
    }

    "getActualPupCodedInCYPlusOne is called" should {
      val employmentTypePrimaryIncome    = 1
      val employmentTypeNotPrimaryIncome = 2
      "return getActualPupCodedInCYPlusOne if there is an income source with an employmentType indicating primary income" in {
        NpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypePrimaryIncome),
              actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("123"))
            )
          )
        ).getActualPupCodedInCYPlusOne shouldBe Some(BigDecimal("123"))
      }

      "return None if there is no income source with an employmentType indicating primary income" in {
        NpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypeNotPrimaryIncome),
              actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("123"))
            )
          )
        ).getActualPupCodedInCYPlusOne shouldBe None
      }

      "return None if employmentType is missing (ASA-265)" in {
        NpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = None,
              actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("123"))
            )
          )
        ).getActualPupCodedInCYPlusOne shouldBe None
      }
    }
  }

  "NpsIncomeSource" when {
    "toTaxAccount is called" should {
      "return IncomeSource if taxCode, employmentType, employmentTaxDistrictNumber, and employmentPayeRef are present" in {
        testNpsIncomeSource.toIncomeSource shouldBe Some(
          IncomeSource(
            employmentId = testNpsIncomeSource.employmentId,
            employmentTaxDistrictNumber = testNpsIncomeSource.employmentTaxDistrictNumber.get,
            employmentPayeRef = testNpsIncomeSource.employmentPayeRef.get,
            actualPUPCodedInCYPlusOneTaxYear = testNpsIncomeSource.actualPUPCodedInCYPlusOneTaxYear,
            deductions = testNpsIncomeSource.deductions,
            allowances = testNpsIncomeSource.allowances,
            employmentType = testNpsIncomeSource.employmentType.get,
            taxCode = testNpsIncomeSource.taxCode.get,
            basisOperation = testNpsIncomeSource.basisOperation
          )
        )
      }
      "return None if there is no taxCode" in {
        testNpsIncomeSource.copy(taxCode = None).toIncomeSource shouldBe None
      }
      "return None if there is no employmentType" in {
        testNpsIncomeSource.copy(employmentType = None).toIncomeSource shouldBe None
      }
      "return None if there is no employmentTaxDistrictNumber" in {
        testNpsIncomeSource.copy(employmentTaxDistrictNumber = None).toIncomeSource shouldBe None
      }
      "return None if there is no employmentPayeRef" in {
        testNpsIncomeSource.copy(employmentPayeRef = None).toIncomeSource shouldBe None
      }
    }
  }

  "Deduction" should {
    val allowanceOrDeduction = AllowanceOrDeduction(
      `type` = 7,
      npsDescription = "employer benefits",
      amount = BigDecimal("65"),
      sourceAmount = Some(BigDecimal("65"))
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(allowanceOrDeduction) shouldBe Json.obj(
          "type"           -> 7,
          "npsDescription" -> "employer benefits",
          "amount"         -> BigDecimal("65"),
          "sourceAmount"   -> Some(BigDecimal("65"))
        )
      }

      "an optional field is missing" in {
        Json.toJson(allowanceOrDeduction.copy(sourceAmount = None)) shouldBe Json.obj(
          "type"           -> 7,
          "npsDescription" -> "employer benefits",
          "amount"         -> BigDecimal("65")
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "type"           -> "employer benefits (007)",
          "adjustedAmount" -> BigDecimal("65"),
          "sourceAmount"   -> Some(BigDecimal("65"))
        )

        json.validate[AllowanceOrDeduction] shouldBe JsSuccess(allowanceOrDeduction)
      }

      "sourceAmount is empty" in {
        val json = Json.obj(
          "type"           -> "employer benefits (007)",
          "adjustedAmount" -> BigDecimal("65")
        )

        json.validate[AllowanceOrDeduction] shouldBe JsSuccess(allowanceOrDeduction.copy(sourceAmount = None))
      }
    }

    "fail to read from json" when {
      "a required field is missing" in {
        Json
          .obj(
            "type"         -> "employer benefits (007)",
            "sourceAmount" -> BigDecimal("65")
          )
          .validate[AllowanceOrDeduction] shouldBe a[JsError]
      }

      "empty json" in {
        Json.obj().validate[AllowanceOrDeduction] shouldBe a[JsError]
      }
    }
  }

  "Allowance" should {
    val allowanceOrDeduction = AllowanceOrDeduction(
      `type` = 7,
      npsDescription = "employer benefits",
      amount = BigDecimal("65"),
      sourceAmount = Some(BigDecimal("65"))
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(allowanceOrDeduction) shouldBe Json.obj(
          "type"           -> 7,
          "npsDescription" -> "employer benefits",
          "amount"         -> BigDecimal("65"),
          "sourceAmount"   -> Some(BigDecimal("65"))
        )
      }

      "an optional field is missing" in {
        Json.toJson(allowanceOrDeduction.copy(sourceAmount = None)) shouldBe Json.obj(
          "type"           -> 7,
          "npsDescription" -> "employer benefits",
          "amount"         -> BigDecimal("65")
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "type"           -> "employer benefits (007)",
          "adjustedAmount" -> BigDecimal("65"),
          "sourceAmount"   -> Some(BigDecimal("65"))
        )

        json.validate[AllowanceOrDeduction] shouldBe JsSuccess(allowanceOrDeduction)
      }

      "sourceAmount is empty" in {
        val json = Json.obj(
          "type"           -> "employer benefits (007)",
          "adjustedAmount" -> BigDecimal("65")
        )

        json.validate[AllowanceOrDeduction] shouldBe JsSuccess(allowanceOrDeduction.copy(sourceAmount = None))
      }
    }

    "fail to read from json" when {
      "a required field is missing" in {
        Json
          .obj(
            "type"         -> "employer benefits (007)",
            "sourceAmount" -> Some(BigDecimal("65"))
          )
          .validate[AllowanceOrDeduction] shouldBe a[JsError]
      }

      "empty json" in {
        Json.obj().validate[AllowanceOrDeduction] shouldBe a[JsError]
      }
    }
  }

  "AllowanceOrDeduction" should {

    "read from NPS API format" when {
      "type is a string with description and code" in {
        val json = Json.obj(
          "type"           -> "state pension/state benefits (001)",
          "adjustedAmount" -> BigDecimal("850"),
          "sourceAmount"   -> BigDecimal("1253")
        )

        val expected = AllowanceOrDeduction(
          `type` = 1,
          npsDescription = "state pension/state benefits",
          amount = BigDecimal("850"),
          sourceAmount = Some(BigDecimal("1253"))
        )

        json.validate[AllowanceOrDeduction] shouldBe JsSuccess(expected)
      }

      "type string has extra whitespace" in {
        val json = Json.obj(
          "type"           -> "  employer benefits   (007)",
          "adjustedAmount" -> BigDecimal("65"),
          "sourceAmount"   -> BigDecimal("65")
        )

        val expected = AllowanceOrDeduction(
          `type` = 7,
          npsDescription = "employer benefits",
          amount = BigDecimal("65"),
          sourceAmount = Some(BigDecimal("65"))
        )

        json.validate[AllowanceOrDeduction] shouldBe JsSuccess(expected)
      }

      "uses adjustedAmount field" in {
        val json = Json.obj(
          "type"           -> "personal allowance (011)",
          "adjustedAmount" -> BigDecimal("11500"),
          "sourceAmount"   -> BigDecimal("11000")
        )

        json.validate[AllowanceOrDeduction].get.amount shouldBe BigDecimal("11500")
      }

      "falls back to amount field when adjustedAmount is missing" in {
        val json = Json.obj(
          "type"         -> "personal allowance (011)",
          "amount"       -> BigDecimal("11500"),
          "sourceAmount" -> BigDecimal("11000")
        )

        json.validate[AllowanceOrDeduction].get.amount shouldBe BigDecimal("11500")
      }

      "sourceAmount is optional" in {
        val json = Json.obj(
          "type"           -> "loan interest (008)",
          "adjustedAmount" -> BigDecimal("11500")
        )

        val expected = AllowanceOrDeduction(
          `type` = 8,
          npsDescription = "loan interest",
          amount = BigDecimal("11500"),
          sourceAmount = None
        )

        json.validate[AllowanceOrDeduction] shouldBe JsSuccess(expected)
      }
    }

    "read from cache format" when {
      "type is an integer and npsDescription is a string" in {
        val json = Json.obj(
          "type"           -> 1,
          "npsDescription" -> "state pension/state benefits",
          "amount"         -> BigDecimal("850"),
          "sourceAmount"   -> BigDecimal("1253")
        )

        val expected = AllowanceOrDeduction(
          `type` = 1,
          npsDescription = "state pension/state benefits",
          amount = BigDecimal("850"),
          sourceAmount = Some(BigDecimal("1253"))
        )

        json.validate[AllowanceOrDeduction] shouldBe JsSuccess(expected)
      }

      "type is integer with no npsDescription" in {
        val json = Json.obj(
          "type"           -> 35,
          "npsDescription" -> "",
          "amount"         -> BigDecimal("180"),
          "sourceAmount"   -> BigDecimal("180")
        )

        val expected = AllowanceOrDeduction(
          `type` = 35,
          npsDescription = "",
          amount = BigDecimal("180"),
          sourceAmount = Some(BigDecimal("180"))
        )

        json.validate[AllowanceOrDeduction] shouldBe JsSuccess(expected)
      }

      "type is integer and npsDescription is missing" in {
        val json = Json.obj(
          "type"         -> 41,
          "amount"       -> BigDecimal("100"),
          "sourceAmount" -> BigDecimal("100")
        )

        val result = json.validate[AllowanceOrDeduction].get
        result.`type`         shouldBe 41
        result.npsDescription shouldBe ""
        result.amount         shouldBe BigDecimal("100")
      }

      "handles all numeric types from MongoDB" in {
        val json = Json.obj(
          "type"           -> 11,
          "npsDescription" -> "personal allowance",
          "amount"         -> 1250,
          "sourceAmount"   -> 1250
        )

        val expected = AllowanceOrDeduction(
          `type` = 11,
          npsDescription = "personal allowance",
          amount = BigDecimal("1250"),
          sourceAmount = Some(BigDecimal("1250"))
        )

        json.validate[AllowanceOrDeduction] shouldBe JsSuccess(expected)
      }
    }

    "handle edge cases" when {
      "type code is three digits with leading zeros" in {
        val json = Json.obj(
          "type"           -> "state pension/state benefits (001)",
          "adjustedAmount" -> BigDecimal("850"),
          "sourceAmount"   -> BigDecimal("1253")
        )

        json.validate[AllowanceOrDeduction].get.`type` shouldBe 1
      }

      "type code is two digits" in {
        val json = Json.obj(
          "type"           -> "Underpayment amount (35)",
          "adjustedAmount" -> BigDecimal("180"),
          "sourceAmount"   -> BigDecimal("180")
        )

        json.validate[AllowanceOrDeduction].get.`type` shouldBe 35
      }

      "type string format is invalid - no parentheses" in {
        val json = Json.obj(
          "type"           -> "invalid format 007",
          "adjustedAmount" -> BigDecimal("65"),
          "sourceAmount"   -> BigDecimal("65")
        )

        val result = json.validate[AllowanceOrDeduction].get
        result.`type`         shouldBe 0
        result.npsDescription shouldBe ""
      }

      "type string format is invalid - no closing parenthesis" in {
        val json = Json.obj(
          "type"           -> "invalid format (007",
          "adjustedAmount" -> BigDecimal("65"),
          "sourceAmount"   -> BigDecimal("65")
        )

        // Should fail gracefully and default to type=0
        val result = json.validate[AllowanceOrDeduction].get
        result.`type`         shouldBe 0
        result.npsDescription shouldBe ""
        result.amount         shouldBe BigDecimal("65")
      }

      "type is neither string nor int (null)" in {
        val json = Json.obj(
          "type"           -> JsNull,
          "adjustedAmount" -> BigDecimal("65"),
          "sourceAmount"   -> BigDecimal("65")
        )

        val result = json.validate[AllowanceOrDeduction].get
        result.`type`         shouldBe 0
        result.npsDescription shouldBe ""
      }
    }

    "write to JSON correctly" when {
      "all fields are present" in {
        val allowanceOrDeduction = AllowanceOrDeduction(
          `type` = 1,
          npsDescription = "state pension/state benefits",
          amount = BigDecimal("850"),
          sourceAmount = Some(BigDecimal("1253"))
        )

        val expected = Json.obj(
          "type"           -> 1,
          "npsDescription" -> "state pension/state benefits",
          "amount"         -> 850,
          "sourceAmount"   -> 1253
        )

        Json.toJson(allowanceOrDeduction) shouldBe expected
      }

      "sourceAmount is None" in {
        val allowanceOrDeduction = AllowanceOrDeduction(
          `type` = 8,
          npsDescription = "loan interest",
          amount = BigDecimal("11500"),
          sourceAmount = None
        )

        val expected = Json.obj(
          "type"           -> 8,
          "npsDescription" -> "loan interest",
          "amount"         -> 11500
        )

        Json.toJson(allowanceOrDeduction) shouldBe expected
      }

      "npsDescription is empty string" in {
        val allowanceOrDeduction = AllowanceOrDeduction(
          `type` = 0,
          npsDescription = "",
          amount = BigDecimal("100"),
          sourceAmount = Some(BigDecimal("100"))
        )

        val expected = Json.obj(
          "type"           -> 0,
          "npsDescription" -> "",
          "amount"         -> 100,
          "sourceAmount"   -> 100
        )

        Json.toJson(allowanceOrDeduction) shouldBe expected
      }
    }

    "round-trip correctly" when {
      "serializing and deserializing from cache format" in {
        val original = AllowanceOrDeduction(
          `type` = 1,
          npsDescription = "state pension/state benefits",
          amount = BigDecimal("850"),
          sourceAmount = Some(BigDecimal("1253"))
        )

        val json         = Json.toJson(original)
        val deserialized = json.validate[AllowanceOrDeduction].get

        deserialized shouldBe original
      }

      "deserializing from NPS and serializing to cache" in {
        val npsJson = Json.obj(
          "type"           -> "state pension/state benefits (001)",
          "adjustedAmount" -> BigDecimal("850"),
          "sourceAmount"   -> BigDecimal("1253")
        )

        val deserialized = npsJson.validate[AllowanceOrDeduction].get
        val cacheJson    = Json.toJson(deserialized)

        cacheJson shouldBe Json.obj(
          "type"           -> 1,
          "npsDescription" -> "state pension/state benefits",
          "amount"         -> 850,
          "sourceAmount"   -> 1253
        )

        // And deserialize from cache format should work
        cacheJson.validate[AllowanceOrDeduction].get shouldBe deserialized
      }
    }

    "fail to read from json" when {
      "amount field is missing" in {
        Json
          .obj(
            "type"         -> "employer benefits (007)",
            "sourceAmount" -> BigDecimal("65")
          )
          .validate[AllowanceOrDeduction] shouldBe a[JsError]
      }

      "empty json" in {
        Json.obj().validate[AllowanceOrDeduction] shouldBe a[JsError]
      }
    }
  }

  "NpsIncomeSource" should {
    val npsIncomeSource = testNpsIncomeSource

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(testNpsIncomeSource) shouldBe Json.obj(
          "employmentId"                     -> testNpsIncomeSourceEmpId,
          "employmentTaxDistrictNumber"      -> Some(testNpsIncomeSourceEmpTDN),
          "employmentPayeRef"                -> Some("AZ00010"),
          "actualPUPCodedInCYPlusOneTaxYear" -> Some(BigDecimal(3.14)),
          "deductions"                       -> testDeductions,
          "allowances"                       -> testAllowances,
          "employmentType"                   -> Some(testNpsIncomeSourceEmpType),
          "taxCode"                          -> Some("TAX1234"),
          "basisOperation"                   -> Some(testNpsIncomeBasisOperation)
        )
      }

      "an optional field is missing" in {
        Json.toJson(testNpsIncomeSource.copy(taxCode = None)) shouldBe Json.obj(
          "employmentId"                     -> testNpsIncomeSourceEmpId,
          "employmentTaxDistrictNumber"      -> Some(testNpsIncomeSourceEmpTDN),
          "employmentPayeRef"                -> Some("AZ00010"),
          "actualPUPCodedInCYPlusOneTaxYear" -> Some(BigDecimal(3.14)),
          "deductions"                       -> testDeductions,
          "allowances"                       -> testAllowances,
          "employmentType"                   -> Some(testNpsIncomeSourceEmpType),
          "basisOperation"                   -> Some(testNpsIncomeBasisOperation)
        )
      }
    }

    "deserialise from JSON" when {
      "all fields are valid" in {
        testNpsIncomeSourceHipJson.validate[NpsIncomeSource] shouldBe JsSuccess(testNpsIncomeSourceHip)
      }

      "an optional field is empty" in {
        (testNpsIncomeSourceHipJson.as[JsObject] - "basisOfOperation")
          .validate[NpsIncomeSource] shouldBe JsSuccess(testNpsIncomeSourceHip.copy(basisOperation = None))
      }
    }

    "fail to read from json" when {
      "there is type mismatch" in {
        Json
          .obj(
            "employmentId"                     -> "testNpsIncomeSourceEmpId",
            "employmentTaxDistrictNumber"      -> Some(testNpsIncomeSourceEmpTDN),
            "employmentPayeRef"                -> Some("AZ00010"),
            "actualPUPCodedInCYPlusOneTaxYear" -> Some(BigDecimal(3.14)),
            "deductions"                       -> testDeductions,
            "allowances"                       -> testAllowances,
            "employmentType"                   -> Some(testNpsIncomeSourceEmpType),
            "taxCode"                          -> Some("TAX1234"),
            "basisOperation"                   -> Some(testNpsIncomeBasisOperation)
          )
          .validate[NpsIncomeSource] shouldBe a[JsError]
      }

      "a required field is missing" in {
        Json
          .obj(
            "employmentTaxDistrictNumber"      -> Some(testNpsIncomeSourceEmpTDN),
            "employmentPayeRef"                -> Some("AZ00010"),
            "actualPUPCodedInCYPlusOneTaxYear" -> Some(BigDecimal(3.14)),
            "deductions"                       -> testDeductions,
            "allowances"                       -> testAllowances,
            "employmentType"                   -> Some(testNpsIncomeSourceEmpType),
            "taxCode"                          -> Some("TAX1234"),
            "basisOperation"                   -> Some(testNpsIncomeBasisOperation)
          )
          .validate[NpsIncomeSource] shouldBe a[JsError]
      }

      "empty json" in {
        Json.obj().validate[NpsIncomeSource] shouldBe a[JsError]
      }
    }
  }

  "NpsTaxAccount" should {
    val npsTaxAccount = NpsTaxAccount(List(testNpsIncomeSource))

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(npsTaxAccount) shouldBe Json.obj(
          "employmentDetailsList" -> List(testNpsIncomeSource)
        )
      }

      "an optional field is missing" in {
        Json.toJson(npsTaxAccount.copy(List(testNpsIncomeSource.copy(taxCode = None)))) shouldBe Json.obj(
          "employmentDetailsList" -> List(testNpsIncomeSource.copy(taxCode = None))
        )
      }
    }

    "deserialise from JSON" when {
      "all fields are valid" in {
        val json = Json.obj("employmentDetailsList" -> Json.arr(testNpsIncomeSourceHipJson))
        json.validate[NpsTaxAccount] shouldBe JsSuccess(NpsTaxAccount(List(testNpsIncomeSourceHip)))
      }

      "an optional field is empty" in {
        val json = Json.obj(
          "employmentDetailsList" -> Json.arr(testNpsIncomeSourceHipJson.as[JsObject] - "basisOfOperation")
        )
        json.validate[NpsTaxAccount] shouldBe JsSuccess(
          NpsTaxAccount(List(testNpsIncomeSourceHip.copy(basisOperation = None)))
        )
      }
    }

    "fail to read from json" when {
      "there is type mismatch" in {
        Json
          .obj(
            "employmentId"                     -> "testNpsIncomeSourceEmpId",
            "employmentTaxDistrictNumber"      -> Some(testNpsIncomeSourceEmpTDN),
            "employmentPayeRef"                -> Some("AZ00010"),
            "actualPUPCodedInCYPlusOneTaxYear" -> Some(BigDecimal(3.14)),
            "deductions"                       -> testDeductions,
            "allowances"                       -> testAllowances,
            "employmentType"                   -> Some(testNpsIncomeSourceEmpType),
            "taxCode"                          -> Some("TAX1234"),
            "basisOperation"                   -> Some(testNpsIncomeBasisOperation)
          )
          .validate[NpsTaxAccount] shouldBe a[JsError]
      }

      "a required field is missing" in {
        Json
          .obj(
            "employmentTaxDistrictNumber"      -> Some(testNpsIncomeSourceEmpTDN),
            "employmentPayeRef"                -> Some("AZ00010"),
            "actualPUPCodedInCYPlusOneTaxYear" -> Some(BigDecimal(3.14)),
            "deductions"                       -> testDeductions,
            "allowances"                       -> testAllowances,
            "employmentType"                   -> Some(testNpsIncomeSourceEmpType),
            "taxCode"                          -> Some("TAX1234"),
            "basisOperation"                   -> Some(testNpsIncomeBasisOperation)
          )
          .validate[NpsTaxAccount] shouldBe a[JsError]
      }

      "empty json" in {
        Json.obj().validate[NpsTaxAccount] shouldBe a[JsError]
      }
    }
  }
}
