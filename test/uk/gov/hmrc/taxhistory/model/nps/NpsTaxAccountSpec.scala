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

  lazy val getTaxAcoountResponseURLDummy: JsValue = loadFile("/json/nps/response/GetTaxAccount.json")

  private val primaryEmploymentId              = 12
  private val actualPupCodedInCYPlusOne        = 240
  private val outStandingDebt                  = 145.75
  private val underPayment                     = 15423.29
  private val sequenceNumberInt                = 6
  private val taDeductionType                  = 32
  private val taAllowanceType                  = 11
  private val taAllowanceSourceAmount          = 11500
  private val testNpsIncomeSourceEmpId         = 6
  private val testNpsIncomeSourceEmpTDN        = 961
  private val testNpsIncomeSourceEmpType       = 23
  private val testNpsIncomeBasisOperation      = 64
  private val incomeSource1DeserialisedEmpId   = 12
  private val incomeSource1DeserialisedEmpType = 1
  private val incomeSource1DeserialisedEmpTDN  = 961

  private val taxAccount = getTaxAcoountResponseURLDummy.as[NpsTaxAccount](NpsTaxAccount.formats)

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
    TaDeduction(
      `type` = taDeductionType,
      npsDescription = "savings income taxable at higher rate",
      amount = BigDecimal("38625"),
      sourceAmount = Some(0)
    )
  )
  private val testAllowances      = List(
    TaAllowance(
      `type` = taAllowanceType,
      npsDescription = "personal allowance",
      amount = BigDecimal("11500"),
      sourceAmount = Some(taAllowanceSourceAmount)
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

  "NpsTaxAccount" when {
    "transforming NPS Get Tax Account API response Json correctly to NpsTaxAccount Model " in {
      taxAccount                              shouldBe a[NpsTaxAccount]
      taxAccount.getPrimaryEmploymentId       shouldBe Some(primaryEmploymentId)
      taxAccount.getActualPupCodedInCYPlusOne shouldBe Some(actualPupCodedInCYPlusOne)
      taxAccount.getOutStandingDebt           shouldBe Some(outStandingDebt)
      taxAccount.getUnderPayment              shouldBe Some(underPayment)
    }

    "deserialising the 'incomeSources' field" should {
      val incomeSource1Deserialised = NpsIncomeSource(
        employmentId = incomeSource1DeserialisedEmpId,
        employmentType = Some(incomeSource1DeserialisedEmpType),
        actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("240")),
        deductions = testDeductions,
        allowances = testAllowances,
        taxCode = Some("K7757"),
        basisOperation = None,
        employmentTaxDistrictNumber = Some(incomeSource1DeserialisedEmpTDN),
        employmentPayeRef = Some("AZ00010")
      )
      val incomeSourcesDeserialised = List(incomeSource1Deserialised)

      val incomeSourcesSerialised = Json.parse(s"""
           | [
           |   {
           |     "employmentId":12,
           |     "employmentType":1,
           |     "actualPUPCodedInCYPlusOneTaxYear":240,
           |     "deductions":[
           |       {"type":32,"npsDescription":"savings income taxable at higher rate","amount":38625,"sourceAmount":0}
           |     ],
           |     "allowances":[
           |       {"type":11,"npsDescription":"personal allowance","amount":11500,"sourceAmount":11500}
           |     ],
           |     "taxCode":"K7757",
           |     "employmentTaxDistrictNumber":961,
           |     "employmentPayeRef":"AZ00010"
           |   }
           | ]
        """.stripMargin)

      "deserialise from an empty json array" in {
        val jsonIncomeSourcesMissing = getTaxAcoountResponseURLDummy.as[JsObject] + ("incomeSources" -> JsArray())
        fromJson[NpsTaxAccount](jsonIncomeSourcesMissing).get.incomeSources shouldBe empty
      }

      "deserialise from a non-empty json array of allowances" in {
        val jsonIncomeSourcesPresent =
          getTaxAcoountResponseURLDummy.as[JsObject] + ("incomeSources"           -> incomeSourcesSerialised)
        fromJson[NpsTaxAccount](jsonIncomeSourcesPresent).get.incomeSources shouldBe incomeSourcesDeserialised
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
          "999",
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
        val targetEmploymentId = 6
        val npsEmployment      = NpsEmployment(
          "AA000000",
          targetEmploymentId,
          "961",
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
      val deductionWithOutstandingDebt = TaDeduction(
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
      val deductionWithUnderpayment       = TaDeduction(
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
}
