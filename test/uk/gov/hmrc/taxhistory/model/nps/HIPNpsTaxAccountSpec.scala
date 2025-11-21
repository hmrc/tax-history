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

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json.fromJson
import play.api.libs.json._
import uk.gov.hmrc.domain.TaxCode
import uk.gov.hmrc.taxhistory.model.api.IncomeSource
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}

import java.time.LocalDate

class HIPNpsTaxAccountSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues with DateUtils {
  lazy val getHipTaxAccountResponseURLDummy: JsValue = loadFile("/json/nps/response/HIPGetTaxAccount.json")

  private val primaryEmploymentId              = 1
  private val actualPupCodedInCYPlusOne        = 99999.98
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
  private val taxAccount: HIPNpsTaxAccount     = getHipTaxAccountResponseURLDummy.as[HIPNpsTaxAccount]

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
  private val testNpsIncomeSource = HIPNpsIncomeSource(
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

  "NpsTaxAccount" when {
    "transforming NPS Get Tax Account API response Json correctly to NpsTaxAccount Model " in {
      taxAccount                              shouldBe a[HIPNpsTaxAccount]
      taxAccount.getPrimaryEmploymentId       shouldBe Some(primaryEmploymentId)
      taxAccount.getActualPupCodedInCYPlusOne shouldBe Some(actualPupCodedInCYPlusOne)
      taxAccount.getOutStandingDebt           shouldBe None
      taxAccount.getUnderPayment              shouldBe None
    }

    "deserialising the 'incomeSources' field" should {
      val incomeSource1Deserialised = HIPNpsIncomeSource(
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
                "employmentId":12,
                "employmentType":1,
                "actualPUPCodedInCYPlusOneTaxYear":240,
                "deductions":[
                  {"type":32,"npsDescription":"savings income taxable at higher rate","amount":38625,"sourceAmount":0}
                ],
                "allowances":[
                  {"type":11,"npsDescription":"personal allowance","amount":11500,"sourceAmount":11500}
                ],
                "taxCode":"K7757",
                "employmentTaxDistrictNumber":961,
                "employmentPayeRef":"AZ00010"
              }
            ]
        """.stripMargin)

      "deserialise from an empty json array" in {
        val jsonIncomeSourcesMissing = getHipTaxAccountResponseURLDummy.as[JsObject] + ("incomeSources" -> JsArray())
        fromJson[NpsTaxAccount](jsonIncomeSourcesMissing).get.incomeSources shouldBe empty
      }

      "deserialise from a non-empty json array of allowances" in {
        val jsonIncomeSourcesPresent =
          getHipTaxAccountResponseURLDummy.as[JsObject] + ("incomeSources" -> incomeSourcesSerialised)
        fromJson[HIPNpsTaxAccount](jsonIncomeSourcesPresent).get.incomeSources
          .take(1)                                                   shouldBe incomeSourcesDeserialised
      }

      "deserialise when 'employmentType' contains a null value (ASA-265)" in {
        val noEmploymentTypeJson = (incomeSourcesSerialised \ 0).as[JsObject] + ("employmentType" -> JsNull)
        fromJson[NpsIncomeSource](noEmploymentTypeJson).get.employmentType shouldBe None
      }

      "deserialise when 'employmentTaxDistrictNumber' contains a null value (ASA-265)" in {
        val noEmplTaxDistrictNumJson =
          (incomeSourcesSerialised \ 0).as[JsObject] + ("employmentTaxDistrictNumber"             -> JsNull)
        fromJson[NpsIncomeSource](noEmplTaxDistrictNumJson).get.employmentTaxDistrictNumber shouldBe None
      }

      "deserialise when 'employmentPayeRef' contains a null value (ASA-265)" in {
        val noEmploymentPayeRefJson = (incomeSourcesSerialised \ 0).as[JsObject] + ("employmentPayeRef" -> JsNull)
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
        jsResult             shouldBe a[JsSuccess[_]]
        jsResult.get.taxCode shouldBe Some(unusualTaxCode)
      }

      "handle missing deductionsDetails and allowancesDetails" should {

        "tolerate a missing deductionsDetails field and default to empty list" in {

          val json = Json.parse(
            """ {  "employmentDetailsList": [  {  "employmentSequenceNumber": 9,  "employmentRecordType": "PRIMARY",  "employerReference": "961/A11111",  "allowancesDetails": [  {  "sourceAmount": 12570,  "adjustedAmount": 12570,  "type": "personal allowance (011)",  "summaryIABDDetailsList": [  { "amount": 12570, "type": "Personal Allowance (PA) (118)" }  ]  }  ],  "payAndTax": { "totalIncomeDetails": { "amount": 41052 } }  }  ] } """.stripMargin
          )

          val account = json.as[HIPNpsTaxAccount]
          account.incomeSources                   should not be empty
          account.incomeSources.head.deductions shouldBe empty
          account.incomeSources.head.allowances   should not be empty
        }

        "tolerate a missing allowancesDetails field and default to empty list" in {

          val json = Json.parse(
            """ {  "employmentDetailsList": [  {  "employmentSequenceNumber": 9,  "employmentRecordType": "PRIMARY",  "employerReference": "961/A11111",  "deductionsDetails": [  { "type": 32, "npsDescription": "desc", "adjustedAmount": 12570, "sourceAmount": 12570 }  ],  "payAndTax": { "totalIncomeDetails": { "amount": 41052 } }  }  ] } """.stripMargin
          )

          val account = json.as[HIPNpsTaxAccount]
          account.incomeSources                   should not be empty
          account.incomeSources.head.allowances shouldBe empty
          account.incomeSources.head.deductions   should not be empty
        }

        "tolerate both deductionsDetails and allowancesDetails missing and default both to empty lists" in {

          val json = Json.parse(
            """ {  "employmentDetailsList": [  {  "employmentSequenceNumber": 9,  "employmentRecordType": "PRIMARY",  "employerReference": "961/A11111",  "payAndTax": { "totalIncomeDetails": { "amount": 41052 } }  }  ] } """.stripMargin
          )

          val account = json.as[HIPNpsTaxAccount]
          account.incomeSources.head.deductions shouldBe empty
          account.incomeSources.head.allowances shouldBe empty
        }

        "parse the real HIPGetTaxAccount.json that omits deductionsDetails" in {

          val real = loadFile("/json/nps/response/HIPGetTaxAccountWithNoDeductions.json").as[HIPNpsTaxAccount]

          real.incomeSources                   should not be empty
          real.incomeSources.head.deductions shouldBe empty
        }
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

          HIPNpsTaxAccount(
            List(
              incomeSource
            )
          ).matchedIncomeSource(npsEmployment).get.employmentId shouldBe targetEmploymentId

          HIPNpsTaxAccount(
            List(
              incomeSource.copy(employmentTaxDistrictNumber = None)
            )
          ).matchedIncomeSource(npsEmployment) shouldBe None

          HIPNpsTaxAccount(
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
        HIPNpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypePrimaryIncome)
            )
          )
        ).getPrimaryEmploymentId shouldBe Some(testNpsIncomeSource.employmentId)
      }

      "return None if there is no income source whose employmentType indicates a primary income" in {
        val employmentTypeNotPrimary = 2
        HIPNpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypeNotPrimary)
            )
          )
        ).getPrimaryEmploymentId shouldBe None
      }

      "work with missing employmentType (ASA-265)" in {
        HIPNpsTaxAccount(
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
        HIPNpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypePrimaryIncome),
              deductions = List(deductionWithOutstandingDebt)
            )
          )
        ).getOutStandingDebt shouldBe deductionWithOutstandingDebt.sourceAmount
      }

      "work with missing employmentType (ASA-265)" in {
        HIPNpsTaxAccount(
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
        HIPNpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypePrimaryIncome),
              deductions = List(deductionWithUnderpayment)
            )
          )
        ).getUnderPayment shouldBe deductionWithUnderpayment.sourceAmount
      }

      "work with missing employmentType (ASA-265)" in {
        HIPNpsTaxAccount(
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
        HIPNpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypePrimaryIncome),
              actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("123"))
            )
          )
        ).getActualPupCodedInCYPlusOne shouldBe Some(BigDecimal("123"))
      }

      "return None if there is no income source with an employmentType indicating primary income" in {
        HIPNpsTaxAccount(
          List(
            testNpsIncomeSource.copy(
              employmentType = Some(employmentTypeNotPrimaryIncome),
              actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("123"))
            )
          )
        ).getActualPupCodedInCYPlusOne shouldBe None
      }

      "return None if employmentType is missing (ASA-265)" in {
        HIPNpsTaxAccount(
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
            deductions = testNpsIncomeSource.deductions.map(AllowanceOrDeduction.toTaDeduction),
            allowances = testNpsIncomeSource.allowances.map(AllowanceOrDeduction.toTaAllowance),
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
}
