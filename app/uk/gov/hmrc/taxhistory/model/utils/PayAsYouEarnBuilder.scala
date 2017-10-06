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

package uk.gov.hmrc.taxhistory.model.utils

import play.Logger
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.api.Employment
import uk.gov.hmrc.taxhistory.model.nps._
import uk.gov.hmrc.taxhistory.model.taxhistory.{Allowance, CompanyBenefit, EarlierYearUpdate, PayAsYouEarnDetails}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

trait PayAsYouEarnBuilder extends Auditable{

  def combineResult(iabdResponse:Either[HttpResponse,List[Iabd]],
                    rtiResponse:Either[HttpResponse,RtiData])
                   (npsEmployments: List[NpsEmployment])(implicit headerCarrier: HeaderCarrier):List[Employment]={

    val iabdsOption = fetchResult(iabdResponse)
    val rtiOption = fetchResult(rtiResponse)

    val employments = npsEmployments.map {
      npsEmployment => {
        val companyBenefits = iabdsOption.map {
          iabds => getMatchedCompanyBenefits(iabds,npsEmployment)
        }
        val rtiEmployments = rtiOption.map {
          rtiData => {
            auditEvent(rtiData,onlyInRTI(rtiData.employments,npsEmployments))("only-in-rti")
            getMatchedRtiEmployments(rtiData, npsEmployment)

          }
        }

        buildEmployment(rtiEmploymentsOption=rtiEmployments,iabdsOption=companyBenefits, npsEmployment: NpsEmployment)
      }
    }

    val allowances= iabdsOption match {
      case None => Nil
      case Some(x) => getAllowances(x)
    }

    employments
    //PayAsYouEarnDetails(employments = employments,allowances = allowances)
  }

  def httpOkWithEmploymentJsonPayload(payload: List[Employment]): HttpResponse = {
    HttpResponse(Status.OK,Some(Json.toJson(payload)))
  }

  def onlyInRTI(rtiEmployments:List[RtiEmployment] , npsEmployments: List[NpsEmployment]):List[RtiEmployment]={
    rtiEmployments.filter(rti => !npsEmployments.exists(nps => isMatch(nps,rti)))
  }

  def isMatch(npsEmployment :NpsEmployment, rtiEmployment :RtiEmployment):Boolean={
    (formatString(rtiEmployment.officeNumber) == formatString(npsEmployment.taxDistrictNumber)) &&
      (rtiEmployment.payeRef == npsEmployment.payeNumber)
  }

  def getMatchedRtiEmployments(rtiData: RtiData, npsEmployments: NpsEmployment)(implicit headerCarrier: HeaderCarrier): List[RtiEmployment] = {

    fetchFilteredList(rtiData.employments){
      (rtiEmployment) =>
        isMatch(npsEmployments, rtiEmployment)
    } match {
      case (matchingEmp :: Nil) =>List(matchingEmp)
      case start :: end => {
        Logger.warn("Multiple matching rti employments found.")
        val subMatches = (start :: end).filter {
          rtiEmployment => {
            rtiEmployment.currentPayId.isDefined &&
              npsEmployments.worksNumber.isDefined &&
              rtiEmployment.currentPayId == npsEmployments.worksNumber
          }
        }
        subMatches match {
          case first :: Nil => List(first)
          case x  => {
            auditEvent(rtiData, x)("miss-match")
            Nil
          }
        }
      }
      case _ => Nil //Auditing will happen in the function onlyInRTI for this case
    }
  }

  private def auditEvent(rtiData: RtiData, x: List[RtiEmployment])(eventType:String)(implicit headerCarrier: HeaderCarrier) = {
    Future {
      for {
        r <- x
      } yield {
        val x = Map(
          "nino" -> rtiData.nino,
          "payeRef" -> r.payeRef,
          "officeNumber" -> r.officeNumber,
          "currentPayId" -> r.currentPayId.fold("")(a => a))
        sendDataEvent("Paye for Agents", detail = x, eventType = eventType)
      }
    }
  }

  def convertRtiEYUToEYU(rtiEmployments: List[RtiEmployment]): List[EarlierYearUpdate] = {
    rtiEmployments.head.earlierYearUpdates.map(eyu => EarlierYearUpdate(eyu.taxablePayDelta,
      eyu.totalTaxDelta,
      eyu.receivedDate)).filter(x =>x.taxablePayEYU != 0 && x.taxEYU != 0)
  }

  def buildEmployment(rtiEmploymentsOption: Option[List[RtiEmployment]], iabdsOption: Option[List[Iabd]], npsEmployment: NpsEmployment): Employment = {

    (rtiEmploymentsOption, iabdsOption) match {

      case (None | Some(Nil), None | Some(Nil)) => Employment(
        employerName = npsEmployment.employerName,
        payeReference = getPayeRef(npsEmployment),
        startDate = npsEmployment.startDate,
        endDate = npsEmployment.endDate)
      case (Some(Nil) | None, Some(y)) => {
        Employment(
          employerName = npsEmployment.employerName,
          payeReference = getPayeRef(npsEmployment),
       //   companyBenefits = getCompanyBenefits(y),
          startDate = npsEmployment.startDate,
          endDate = npsEmployment.endDate)
      }
      case (Some(x), Some(Nil) | None) => {
        val rtiPaymentInfo = getRtiPayment(x)
        Employment(
          employerName = npsEmployment.employerName,
          payeReference = getPayeRef(npsEmployment),
        //  taxablePayTotal = rtiPaymentInfo._1,
         // taxTotal = rtiPaymentInfo._2,
        //  earlierYearUpdates = convertRtiEYUToEYU(x),
          startDate = npsEmployment.startDate,
          endDate = npsEmployment.endDate
        )
      }
      case (Some(x), Some(y)) => {
        val rtiPaymentInfo = getRtiPayment(x)
        Employment(
          employerName = npsEmployment.employerName,
          payeReference = getPayeRef(npsEmployment),
        //  taxablePayTotal = rtiPaymentInfo._1,
        //  taxTotal = rtiPaymentInfo._2,
        //  earlierYearUpdates = convertRtiEYUToEYU(x),
        //  companyBenefits = getCompanyBenefits(y),
          startDate = npsEmployment.startDate,
          endDate = npsEmployment.endDate)
      }
      case _ => Employment(
        employerName = npsEmployment.employerName,
        payeReference = getPayeRef(npsEmployment),
        startDate = npsEmployment.startDate,
        endDate = npsEmployment.endDate)
    }

  }

  private def getPayeRef(npsEmployment: NpsEmployment) = {
    npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber
  }

  def getRtiPayment(rtiEmployments: List[RtiEmployment])={
    rtiEmployments.head.payments match {
      case Nil => (None,None)
      case matchingPayments => {
        val payment = matchingPayments.sorted.last
        (Some(payment.taxablePayYTD), Some(payment.totalTaxYTD))
      }
    }
  }

  def getMatchedCompanyBenefits(iabds:List[Iabd],npsEmployment: NpsEmployment):List[Iabd] = {
    fetchFilteredList(getRawCompanyBenefits(iabds)){
      (iabd) => {
        iabd.employmentSequenceNumber.contains(npsEmployment.sequenceNumber)
      }
    }
  }

  def getRawCompanyBenefits(iabds:List[Iabd]):List[Iabd] = {

    fetchFilteredList(iabds){
      iabd => {
        iabd.`type`.isInstanceOf[CompanyBenefits]
      }
    }


  }

  def getCompanyBenefits(iabds:List[Iabd]):List[CompanyBenefit] = {
    if (isTotalBenefitInKind(iabds)) {

      convertToCompanyBenefits(iabds)

    }else{
      convertToCompanyBenefits(fetchFilteredList(iabds) {
        iabd => {
          !iabd.`type`.isInstanceOf[TotalBenefitInKind.type]
        }
      }
      )
    }
  }

  def convertToCompanyBenefits(iabds:List[Iabd]):List[CompanyBenefit]= {
    iabds.map {
      iabd =>
        CompanyBenefit(typeDescription =
          iabd.typeDescription.fold {
            Logger.warn("Iabds Description is blank")
            ""
          }(x => x),
          amount = iabd.grossAmount.fold {
            Logger.warn("Iabds grossAmount is blank")
            BigDecimal(0)
          }(x => x),
          iabdMessageKey = iabd.`type`.toString)
    }
  }

  def isTotalBenefitInKind(iabds:List[Iabd]):Boolean = {
    fetchFilteredList(iabds) {
      iabd => {
        iabd.`type`.isInstanceOf[TotalBenefitInKind.type]
      }
    }.nonEmpty &&
      fetchFilteredList(iabds) {
        iabd => {
          iabd.`type`.isInstanceOf[uk.gov.hmrc.taxhistory.model.nps.BenefitInKind]
        }
      }.size == 1
  }


  def getRawAllowances(iabds:List[Iabd]):List[Iabd] = {

    fetchFilteredList(iabds){
      iabd => {
        iabd.`type`.isInstanceOf[Allowances]
      }
    }
  }

  def getAllowances(iabds:List[Iabd]):List[Allowance] = {
    getRawAllowances(iabds) map {
      iabd => Allowance(typeDescription =
        iabd.typeDescription.fold{
          Logger.warn("Iabds Description is blank")
          ""}(x=>x),
        amount = iabd.grossAmount.fold{
          Logger.warn("Iabds grossAmount is blank")
          BigDecimal(0)
        }(x=>x),
        iabdMessageKey = iabd.`type`.toString)
    }
  }

  def fetchResult[A,B](either:Either[A,B]):Option[B]={
    either match {
      case Left(x) => None
      case Right(x) => Some(x)
    }
  }


  def fetchFilteredList[A](listToFilter:List[A])(f:(A) => Boolean):List[A] = {
    listToFilter.filter(f(_))
  }

  def formatString(a: String):String = {
    Try(a.toInt) match {
      case Success(x) => x.toString
      case Failure(y) => a
    }
  }

}
