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

package uk.gov.hmrc.taxhistory.model.nps

import java.util.UUID

import org.joda.time.LocalDate
import play.api.libs.json.Json.{fromJson, toJson}
import play.api.libs.json._
import uk.gov.hmrc.domain.TaxCode
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.api.TaxAccount
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class NpsTaxAccountSpec extends TestUtil with UnitSpec {

  lazy val getTaxAcoountResponseURLDummy: JsValue = loadFile("/json/nps/response/GetTaxAccount.json")

  private val primaryEmploymentId = 12
  private val actualPupCodedInCYPlusOne = 240
  private val outStandingDebt = 145.75
  private val underPayment = 15423.29

  private val taxAccount = getTaxAcoountResponseURLDummy.as[NpsTaxAccount](NpsTaxAccount.formats)

  private val testNpsEmployment = NpsEmployment(
    nino = "AA000000",
    sequenceNumber = 6,
    taxDistrictNumber = "961",
    payeNumber = "AZ00010",
    employerName = "Aldi",
    worksNumber = Some("6044041000000"),
    receivingJobSeekersAllowance = false,
    otherIncomeSourceIndicator = false,
    startDate = new LocalDate("2015-01-21"),
    endDate = None,
    receivingOccupationalPension = false,
    employmentStatus = Live
  )

  private val testIncomeSource = IncomeSource(
    employmentId = 6,
    employmentTaxDistrictNumber = Some(961),
    employmentPayeRef = Some("AZ00010"),
    actualPUPCodedInCYPlusOneTaxYear = None,
    deductions = Nil,
    allowances = Nil,
    employmentType = None,
    taxCode = None,
    basisOperation = None
  )

  "NpsTaxAccount" when {
    "transforming NPS Get Tax Account API Response Json correctly to NpsTaxAccount Model" in {
      taxAccount shouldBe a[NpsTaxAccount]
      taxAccount.getPrimaryEmploymentId shouldBe Some(primaryEmploymentId)
      taxAccount.getActualPupCodedInCYPlusOne shouldBe Some(actualPupCodedInCYPlusOne)
      taxAccount.getOutStandingDebt shouldBe Some(outStandingDebt)
      taxAccount.getUnderPayment shouldBe Some(underPayment)
    }

    "(de)serialising the 'incomeSources' field" should {
      val incomeSource1Deserialised = IncomeSource(
        employmentId = 12,
        employmentType = Some(1),
        actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("240")),
        deductions = List(
          TaDeduction(`type` = 32, npsDescription = "savings income taxable at higher rate", amount = BigDecimal("38625"), sourceAmount = Some(0))
        ),
        allowances = List(
          TaAllowance(`type` = 11, npsDescription = "personal allowance", amount = BigDecimal("11500"), sourceAmount = Some(11500))
        ),
        taxCode = Some("K7757"),
        basisOperation = None,
        employmentTaxDistrictNumber = Some(961),
        employmentPayeRef = Some("AZ00010")
      )
      val incomeSourcesDeserialised = List(incomeSource1Deserialised)

      val incomeSourcesSerialised = Json.parse(
        s"""
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

      "serialise to an empty json array value when there are no income sources" in {
        val incomeSourcesMissing = NpsTaxAccount(incomeSources = Nil)
        (toJson(incomeSourcesMissing) \ "incomeSources") shouldBe JsDefined(JsArray(Nil))
      }
      "serialise to a json array of income source objects when there are income sources" in {
        val incomeSourcesPresent = NpsTaxAccount(incomeSources = incomeSourcesDeserialised)
        (toJson(incomeSourcesPresent) \ "incomeSources") shouldBe JsDefined(incomeSourcesSerialised)
      }
      "deserialise from an empty json array" in {
        val jsonIncomeSourcesMissing = getTaxAcoountResponseURLDummy.as[JsObject] + ("incomeSources" -> JsArray())
        fromJson[NpsTaxAccount](jsonIncomeSourcesMissing).get.incomeSources shouldBe empty
      }
      "deserialise from a non-empty json array of allowances" in {
        val jsonIncomeSourcesPresent = getTaxAcoountResponseURLDummy.as[JsObject] + ("incomeSources" -> incomeSourcesSerialised)
        fromJson[NpsTaxAccount](jsonIncomeSourcesPresent).get.incomeSources shouldBe incomeSourcesDeserialised
      }

      "serialise when 'employmentType' contains a null value (ASA-265)" in {
        val noEmploymentTypeJson = (incomeSourcesSerialised \ 0).as[JsObject] + ("employmentType" -> JsNull)
        fromJson[IncomeSource](noEmploymentTypeJson).get.employmentType shouldBe None
      }

      "serialise when 'employmentTaxDistrictNumber' contains a null value (ASA-265)" in {
        val noEmplTaxDistrictNumJson = (incomeSourcesSerialised \ 0).as[JsObject] + ("employmentTaxDistrictNumber" -> JsNull)
        fromJson[IncomeSource](noEmplTaxDistrictNumJson).get.employmentTaxDistrictNumber shouldBe None
      }

      "serialise when 'employmentPayeRef' contains a null value (ASA-265)" in {
        val noEmploymentPayeRefJson = (incomeSourcesSerialised \ 0).as[JsObject] + ("employmentPayeRef" -> JsNull)
        fromJson[IncomeSource](noEmploymentPayeRefJson).get.employmentPayeRef shouldBe None
      }

      "serialise when 'taxCode' contains a null value (ASA-265)" in {
        val noTaxCodeJson = (incomeSourcesSerialised \ 0).as[JsObject] + ("taxCode" -> JsNull)
        fromJson[IncomeSource](noTaxCodeJson).get.taxCode shouldBe None
      }

      "serialise when 'taxCode' contains a value that is not valid for the TaxCode model class (ASA-265)" in {
        val unusualTaxCode = "FNORDS"
        assertThrows[IllegalArgumentException]{ TaxCode(unusualTaxCode) }

        val withUnusualTaxCodeJson = (incomeSourcesSerialised \ 0).as[JsObject] + ("taxCode" -> JsString(unusualTaxCode))
        val jsResult = fromJson[IncomeSource](withUnusualTaxCodeJson)
        jsResult shouldBe a[JsSuccess[_]]
        jsResult.get.taxCode shouldBe Some(unusualTaxCode)
      }
    }

    "matchedIncomeSource is called" should {
      "return None when no emnployment and income source match" in {
        val npsEmployment = NpsEmployment(
          "AA000000", 6, "999", "AZ00010", "Aldi", Some("6044041000000"), receivingJobSeekersAllowance = false,
          otherIncomeSourceIndicator = false, new LocalDate("2015-01-21"), None, receivingOccupationalPension = false, Live)

        taxAccount.matchedIncomeSource(npsEmployment) shouldBe None
      }

      "return a single income source when income sources match the employment" in {
        val targetEmploymentId = 6
        val npsEmployment = NpsEmployment(
          "AA000000", targetEmploymentId, "961", "AZ00010", "Aldi", Some("6044041000000"), receivingJobSeekersAllowance = false,
          otherIncomeSourceIndicator = false, new LocalDate("2015-01-21"), None, receivingOccupationalPension = false, Live)

        taxAccount.matchedIncomeSource(npsEmployment).get.employmentId shouldBe targetEmploymentId
      }

      "return None if the income source's would normally match the employment but the employmentTaxDistrictNumber or employmentPayeRef is a None but (ASA-265)" in {
        val targetEmploymentId = 6
        val matchingTaxDistrictNum = 961
        val matchingEmploymentRef = "AZ00010"
        val npsEmployment = testNpsEmployment.copy(
          sequenceNumber = targetEmploymentId,
          taxDistrictNumber = matchingTaxDistrictNum.toString,
          payeNumber = matchingEmploymentRef
        )

        val incomeSource = testIncomeSource.copy(
          employmentId = targetEmploymentId,
          employmentTaxDistrictNumber = Some(matchingTaxDistrictNum),
          employmentPayeRef = Some(matchingEmploymentRef)
        )

        NpsTaxAccount(List(
          incomeSource
        )).matchedIncomeSource(npsEmployment).get.employmentId shouldBe targetEmploymentId

        NpsTaxAccount(List(
          incomeSource.copy(employmentTaxDistrictNumber = None)
        )).matchedIncomeSource(npsEmployment) shouldBe None

        NpsTaxAccount(List(
          incomeSource.copy(employmentPayeRef = None)
        )).matchedIncomeSource(npsEmployment) shouldBe None
      }
    }

    "getPrimaryEmploymentId is called" should {
      "return the employmentId if there is an IncomeSource with a PrimaryIncome employmentType" in {
        val employmentTypePrimaryIncome = 1
        NpsTaxAccount(List(testIncomeSource.copy(
          employmentType = Some(employmentTypePrimaryIncome)
        ))).getPrimaryEmploymentId shouldBe Some(testIncomeSource.employmentId)
      }

      "return None if there is no IncomeSource with a PrimaryIncome employmentType" in {
        val employmentTypeNotPrimary = 2
        NpsTaxAccount(List(testIncomeSource.copy(
          employmentType = Some(employmentTypeNotPrimary)
        ))).getPrimaryEmploymentId shouldBe None
      }

      "work with missing employmentType (ASA-265)" in {
        NpsTaxAccount(List(testIncomeSource.copy(
          employmentType = None
        ))).getPrimaryEmploymentId shouldBe None
      }
    }

    "getOutStandingDebt is called" should {
      val employmentTypePrimaryIncome = 1
      val deductionTypeOutstandingDebt = 41
      val deductionWithOutstandingDebt = TaDeduction(
        `type` = deductionTypeOutstandingDebt,
        npsDescription = "desc",
        amount = BigDecimal("123"),
        sourceAmount = Some(BigDecimal("456"))
      )

      "return the sourceAmount from a primary income with a deduction indicating outstanding debt" in {
        NpsTaxAccount(List(testIncomeSource.copy(
          employmentType = Some(employmentTypePrimaryIncome),
          deductions = List(deductionWithOutstandingDebt)
        ))).getOutStandingDebt shouldBe deductionWithOutstandingDebt.sourceAmount
      }

      "work with missing employmentType (ASA-265)" in {
        NpsTaxAccount(List(testIncomeSource.copy(
          employmentType = None,
          deductions = List(deductionWithOutstandingDebt)
        ))).getOutStandingDebt shouldBe None
      }
    }

    "getUnderPayment is called" should {
      val employmentTypePrimaryIncome = 1
      val deductionTypeUnderpaymentAmount = 35
      val deductionWithUnderpayment = TaDeduction(
        `type` = deductionTypeUnderpaymentAmount,
        npsDescription = "desc",
        amount = BigDecimal("123"),
        sourceAmount = Some(BigDecimal("456"))
      )

      "return the sourceAmount from a primary income with a deduction indicating an underpayment amount" in {
        NpsTaxAccount(List(testIncomeSource.copy(
          employmentType = Some(employmentTypePrimaryIncome),
          deductions = List(deductionWithUnderpayment)
        ))).getUnderPayment shouldBe deductionWithUnderpayment.sourceAmount
      }

      "work with missing employmentType (ASA-265)" in {
        NpsTaxAccount(List(testIncomeSource.copy(
          employmentType = None,
          deductions = List(deductionWithUnderpayment)
        ))).getUnderPayment shouldBe None
      }
    }

    "getActualPupCodedInCYPlusOne is called" should {
      val employmentTypePrimaryIncome = 1
      "return getActualPupCodedInCYPlusOne if there is an IncomeSource with a PrimaryIncome employmentType" in {
        NpsTaxAccount(List(testIncomeSource.copy(
          employmentType = Some(employmentTypePrimaryIncome),
          actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("123"))
        ))).getActualPupCodedInCYPlusOne shouldBe Some(BigDecimal("123"))
      }

      "return getActualPupCodedInCYPlusOne if there is no IncomeSource with a PrimaryIncome employmentType" in {
        NpsTaxAccount(List(testIncomeSource.copy(
          employmentType = Some(employmentTypePrimaryIncome + 1),
          actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("123"))
        ))).getActualPupCodedInCYPlusOne shouldBe None
      }


      "return None if employmentType is missing (ASA-265)" in {
        NpsTaxAccount(List(testIncomeSource.copy(
          employmentType = None,
          actualPUPCodedInCYPlusOneTaxYear = Some(BigDecimal("123"))
        ))).getActualPupCodedInCYPlusOne shouldBe None
      }
    }
  }
}