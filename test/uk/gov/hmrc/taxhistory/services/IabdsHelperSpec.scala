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

import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live
import uk.gov.hmrc.taxhistory.model.nps.{Iabd, NpsEmployment, VanBenefit}
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.helpers.IabdsHelper


class IabdsHelperSpec extends PlaySpec with MockitoSugar with TestUtil {

  implicit val hc = HeaderCarrier()
  val testNino = randomNino()

  val onlyIabdList:List[Iabd]= List(Iabd("QQ000003", Some(201700055), VanBenefit, Some(100), Some("Van Benefit"),Some(26)))

  val npsEmploymentResponse :List[NpsEmployment] = List(
    NpsEmployment(
      "AA000000", 1, "531", "J4816", "Aldi", Some("6044041000000"), false, false,
      new LocalDate("2015-01-21"), None, false, Live))


  lazy val iabdsJsonResponse = loadFile("/json/nps/response/iabds.json")
  lazy val iabdList = iabdsJsonResponse.as[List[Iabd]]
  lazy val iabdsTotalBenfitInKindJsonResponse = loadFile("/json/nps/response/iabdsTotalBIK.json")
  lazy val iabdsBenfitInKindJsonResponse = loadFile("/json/nps/response/iabdsBIK.json")


  "Iabds Helper" should {
    "correctly convert an iabd to an allowance model" in {
      val iabdsHelper = new IabdsHelper(iabdList)
      val allowances =  iabdsHelper.getAllowances()
      allowances.size mustBe 1
    }
    "Return an empty list of allowances when only iabd is present" in {
      val iabdsHelper = new IabdsHelper(onlyIabdList)

      val allowances =  iabdsHelper.getAllowances()
      allowances.size mustBe 0
    }

    "Return a filtered Iabds from  List of Nps Iabds" in {

      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val iabdsFiltered = iabdsHelper.getRawCompanyBenefits()
      iabdsFiltered.size mustBe 4
      iabdsFiltered.toString() contains  ("FlatRateJobExpenses") mustBe false
      iabdsFiltered.toString() contains  ("VanBenefit")  mustBe true
      iabdsFiltered.toString() contains  ("CarFuelBenefit")  mustBe true
    }

    "Return a matched iabds  from  List of employments" in {
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)


      val matchedIabds = iabdsHelper.getMatchedCompanyBenefits(npsEmploymentResponse.head)
      matchedIabds.size mustBe 2
      matchedIabds.toString() contains  ("VanBenefit") mustBe true
      matchedIabds.toString() contains  ("CarFuelBenefit") mustBe true
    }

    "Get CompanyBenfits from Iabd data and ignore Benefit In Kind (type 28)" in {

      val iabds = iabdsBenfitInKindJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val companyBenefits=iabdsHelper.getCompanyBenefits()
      companyBenefits.size mustBe  7
      val companyBenefit1 = companyBenefits.head
      companyBenefit1.source mustBe Some(26)
      companyBenefit1.iabdType mustBe "UnKnown"
      companyBenefit1.amount mustBe 36795
    }

    "Get CompanyBenfits from Iabd data Benefit In Kind of type 28(Total Benefit In Kind)" in {

      val iabds = iabdsTotalBenfitInKindJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val companyBenefits=iabdsHelper.getCompanyBenefits()
      companyBenefits.size mustBe  2
    }

    "Total Benefit In Kind  from Iabds list should return true if There is only BIK which is type 28" in {
      val iabds = iabdsTotalBenfitInKindJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val bik=iabdsHelper.isTotalBenefitInKind()
      bik  mustBe  true
    }

    "Total Benefit In Kind  from Iabds list should return false if There is any BIK which is not type 28" in {
      val iabds = iabdsBenfitInKindJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val bik=iabdsHelper.isTotalBenefitInKind()
      bik  mustBe  false
    }

    "Return only Allowances from  List of Nps Iabds" in {
      val iabds = iabdsJsonResponse.as[List[Iabd]]
      val iabdsHelper = new IabdsHelper(iabds)

      val iabdsFiltered = iabdsHelper.getRawAllowances()
      iabdsFiltered.size mustBe 1
      iabdsFiltered.toString() contains  ("FlatRateJobExpenses") mustBe true
    }

  }
}
