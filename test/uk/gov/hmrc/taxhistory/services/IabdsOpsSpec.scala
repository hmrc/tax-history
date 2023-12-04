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

package uk.gov.hmrc.taxhistory.services

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, VanBenefit}
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate

class IabdsOpsSpec extends PlaySpec with MockitoSugar with TestUtil with DateUtils {

  import uk.gov.hmrc.taxhistory.services.helpers.IabdsOps._

  implicit val hc: HeaderCarrier       = HeaderCarrier()
  private val employmentSequenceNumber = 201700055
  private val grossAmount              = 100
  private val paymentFrequency         = 26

  val onlyIabdList: List[Iabd] = List(
    Iabd(
      nino = "QQ000003",
      employmentSequenceNumber = Some(employmentSequenceNumber),
      `type` = VanBenefit,
      grossAmount = Some(grossAmount),
      typeDescription = Some("Van Benefit"),
      captureDate = Some("5/4/2022"),
      paymentFrequency = Some(paymentFrequency)
    )
  )

  val npsEmploymentResponse: List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000",
      1,
      "531",
      "J4816",
      "Aldi",
      Some("6044041000000"),
      receivingJobSeekersAllowance = false,
      otherIncomeSourceIndicator = false,
      Some(LocalDate.of(YEAR_2015, JANUARY, DAY_21)),
      None,
      receivingOccupationalPension = false,
      Live
    )
  )

  lazy val iabdList: List[Iabd]                                   = loadFile("/json/nps/response/iabds.json").as[List[Iabd]]
  lazy val iabdsTotalBenfitInKindJsonResponse: JsValue            = loadFile("/json/nps/response/iabdsTotalBIK.json")
  lazy val iabdsBenfitInKindJsonResponse: JsValue                 = loadFile("/json/nps/response/iabdsBIK.json")
  lazy val iabdsBenfitInKindNoGrossJsonResponse: JsValue          = loadFile("/json/nps/response/iabdsBIKNoGrossAmount.json")
  lazy val iabdsBenfitInKindAllowanceNoGrossJsonResponse: JsValue = loadFile(
    "/json/nps/response/iabdsAllowanceNoGross.json"
  )

  "Iabds Helper" should {
    "correctly convert an iabd to an allowance model" in {
      val allowances = iabdList.allowances
      allowances.size mustBe 1
    }
    "Return an empty list of allowances when only iabd is present" in {
      val allowances = onlyIabdList.allowances
      allowances.size mustBe 0
    }

    "Return a matched iabds from List of employments" in {

      val matchedIabds = iabdList.matchedCompanyBenefits(npsEmploymentResponse.head)
      matchedIabds.size mustBe 2
      matchedIabds.toString contains "VanBenefit" mustBe true
      matchedIabds.toString contains "CarFuelBenefit" mustBe true
    }

    "Get CompanyBenfits from Iabd data and ignore Benefit In Kind (type 28)" in {

      val cbSource = 26
      val iabds    = iabdsBenfitInKindJsonResponse.as[List[Iabd]]

      val companyBenefits = iabds.companyBenefits(TaxYear(2022))
      companyBenefits.size mustBe 7
      val companyBenefit1 = companyBenefits.head
      companyBenefit1.source mustBe Some(cbSource)
      companyBenefit1.iabdType mustBe "UnKnown"
      companyBenefit1.amount mustBe 36795
    }

    "Get CompanyBenfits from Iabd data Benefit In Kind of type 28(Total Benefit In Kind)" in {

      val iabds = iabdsTotalBenfitInKindJsonResponse.as[List[Iabd]]

      val companyBenefits = iabds.companyBenefits(TaxYear(2022))
      companyBenefits.size mustBe 2
    }

    "Total Benefit In Kind from Iabds list should return true if There is only BIK which is type 28" in {
      val iabds = iabdsTotalBenfitInKindJsonResponse.as[List[Iabd]]

      val bik = iabds.isTotalBenefitInKind
      bik mustBe true
    }

    "Total Benefit In Kind from Iabds list should return false if There is any BIK which is not type 28" in {
      val iabds = iabdsBenfitInKindJsonResponse.as[List[Iabd]]

      val bik = iabds.isTotalBenefitInKind
      bik mustBe false
    }

    "Return only Allowances from List of Nps Iabds" in {
      val allowances           = iabdList.allowances
      val expectedGross        = 200
      val expectedGrossDecimal = BigDecimal(expectedGross)

      allowances.size mustBe 1
      allowances.head.amount mustBe expectedGrossDecimal
      allowances.toString() contains "FlatRateJobExpenses" mustBe true
    }

    "Return Iabds converted to company benefits when no gross amount default it to 0" in {
      val ninoWithoutGrossAmount = Set("QQ000001", "QQ000002")
      val iabds                  = iabdsBenfitInKindNoGrossJsonResponse.as[List[Iabd]]
      val targetIabds            = iabds.filter(iabd => ninoWithoutGrossAmount.contains(iabd.nino))
      val companyBenefits        = targetIabds.companyBenefits(TaxYear(2022))

      companyBenefits.size mustBe 2
      companyBenefits.head.amount mustBe BigDecimal(0)
      companyBenefits.last.amount mustBe BigDecimal(0)
    }

    "Return only Allowances from List of Nps Iabds, when no gross it defaults to 0" in {
      val allowances    = iabdsBenfitInKindAllowanceNoGrossJsonResponse.as[List[Iabd]].allowances
      val expectedGross = BigDecimal(0)

      allowances.size mustBe 1
      allowances.head.amount mustBe expectedGross
      allowances.toString() contains "FlatRateJobExpenses" mustBe true
    }

  }
}
