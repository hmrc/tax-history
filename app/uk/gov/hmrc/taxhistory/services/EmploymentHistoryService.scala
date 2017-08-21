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

package uk.gov.hmrc.taxhistory.services

import play.Logger
import play.api.http.Status
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment, RtiPayment}
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.nps.{Allowances, CompanyBenefits, Iabd, NpsEmployment}
import uk.gov.hmrc.time.TaxYear
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.taxhistory.model.taxhistory.{Allowance, CompanyBenefit, Employment, PayAsYouEarnDetails}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object EmploymentHistoryService extends EmploymentHistoryService

trait EmploymentHistoryService {
  def npsConnector : NpsConnector = NpsConnector
  def rtiConnector : RtiConnector = RtiConnector

   def formatString(a: String):String = {
      Try(a.toInt) match {
        case Success(x) => x.toString
        case Failure(y) => a
      }
    }



  def getEmploymentHistory(nino:String, taxYear:Int)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val validatedNino = Nino(nino)
    val validatedTaxYear = TaxYear(taxYear)

    val x = for {
      npsEmploymentsFuture <- getNpsEmployments(validatedNino, validatedTaxYear)
    }
      yield {
        npsEmploymentsFuture match {
          case Left(httpResponse) =>Future.successful(httpResponse)
          case Right(Nil) => Future.successful(HttpResponse(Status.NOT_FOUND, Some(Json.parse("""{"Message":"Not Found"}"""))))
          case Right(npsEmploymentList) => {
                getPayAsYouEarnDetails(validatedNino,validatedTaxYear)(npsEmploymentList)
           // handleNpsEmploymentList(validatedNino, validatedTaxYear)(npsEmploymentList, createEmploymentList)
          }
        }
      }
    x.flatMap(identity)
  }


  def getPayAsYouEarnDetails(nino: Nino,taxYear: TaxYear)(npsEmployments: List[NpsEmployment])(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    for {
      iabdsF <- getNpsIabds(nino,taxYear)
      rtiF <- getRtiEmployments(nino,taxYear)

    }yield {
      combineResult(iabdsF,rtiF)(npsEmployments)
    }

  }

  def combineResult(iabdResponse:Either[HttpResponse,List[Iabd]],
                    rtiResponse:Either[HttpResponse,RtiData])
                   (npsEmployments: List[NpsEmployment])(implicit headerCarrier: HeaderCarrier):HttpResponse={

    val iabdsOption = fetchResult(iabdResponse)
    val rtiOption = fetchResult(rtiResponse)
    val employments = npsEmployments.map {
      npsEmployment => {
        val companyBenefits = iabdsOption.map {
            iabds => getMatchedCompanyBenefits(iabds,npsEmployment)
        }
        val rtiEmployments = rtiOption.map {
            rtiData => getMatchedRtiEmployments(rtiData,npsEmployment)
        }

        buildEmployment(rtiEmploymentsOption=rtiEmployments,iabdsOption=companyBenefits, npsEmployment: NpsEmployment)
      }
    }

    val allowances= iabdsOption match {
      case None => Nil
      case Some(x) => getAllowances(x)
    }

    val payAsYouEarnDetails = PayAsYouEarnDetails(employments = employments,allowances = allowances)

    HttpResponse(Status.OK,Some(Json.toJson(payAsYouEarnDetails)))
  }


  def fetchResult[A,B](either:Either[A,B]):Option[B]={
    either match {
      case Left(x) => None
      case Right(x) => Some(x)
    }
  }

  def handleNpsEmploymentList(nino:Nino,taxYear:TaxYear)
                             (npsEmploymentList:List[NpsEmployment],
                              mergeEmployments: (Option[RtiData],List[NpsEmployment]) => List[Employment])
                             (implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    for(rtiDataFuture <- getRtiEmployments(nino, taxYear))
      yield {
        rtiDataFuture match {
          case Left(httpResponse) => HttpResponse(Status.OK,
            Some(Json.toJson(mergeEmployments(None,npsEmploymentList))))
          case Right(rtiData) => {
            HttpResponse(Status.OK,
              Some(Json.toJson(mergeEmployments(Some(rtiData),npsEmploymentList))))
          }
        }
      }
  }


  def fetchFilteredList[A](listToFilter:List[A])(f:(A) => Boolean):List[A] = {
    listToFilter.filter(f(_))
  }

  def getMatchedRtiEmployments(rtiData:RtiData, npsEmployment: NpsEmployment): List[RtiEmployment] = {

      fetchFilteredList(rtiData.employments){
        (rtiEmployment) =>
          (formatString(rtiEmployment.officeNumber) == formatString(npsEmployment.taxDistrictNumber)) &&
            rtiEmployment.payeRef == npsEmployment.payeNumber
      } match {
        case (matchingEmp :: Nil) =>List(matchingEmp)
        case start :: end => {
            Logger.warn("Multiple matching rti employments found.")
            val subMatches = (start :: end).filter {
              rtiEmployment => {
                rtiEmployment.currentPayId.isDefined &&
                  npsEmployment.worksNumber.isDefined &&
                  rtiEmployment.currentPayId == npsEmployment.worksNumber
              }
            }
            subMatches match {
              case first :: Nil => List(first)
              case _ => Nil
            }
          }
        case _ => Nil
      }
  }



  def createEmploymentList(rtiData:Option[RtiData], npsEmployments: List[NpsEmployment]): List[Employment] = {

    npsEmployments.flatMap {
      npsEmployment => {
        val f = rtiData.map(
          data =>
            data.employments.filter {
              rtiEmployment => {
                (formatString(rtiEmployment.officeNumber) == formatString(npsEmployment.taxDistrictNumber)) &&
                  rtiEmployment.payeRef == npsEmployment.payeNumber
              }
            }
        )

        f match {
          case None => buildEmployment(Nil, npsEmployment)
          case Some(Nil) => buildEmployment(Nil, npsEmployment)
          case Some(matchingEmp :: Nil) => buildEmployment(matchingEmp.payments, npsEmployment)
          case Some(start :: end) => {
            Logger.warn("Multiple matching rti employments found.")
            val subMatches = (start :: end).filter {
              rtiEmployment => {
                rtiEmployment.currentPayId.isDefined &&
                  npsEmployment.worksNumber.isDefined &&
                  rtiEmployment.currentPayId == npsEmployment.worksNumber
              }
            }
            subMatches match {
              case first :: Nil => buildEmployment(first.payments, npsEmployment)
              case _ => buildEmployment(Nil, npsEmployment)
            }
          }
        }
      }
    }

  }

  def buildEmployment(payments:List[RtiPayment], npsEmployment: NpsEmployment): Option[Employment] = {
    payments.sorted match {
      case Nil =>  Some(Employment(
        employerName = npsEmployment.employerName,
        payeReference = npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber))
      case matchingPayments => {
        val payment = matchingPayments.sorted.last
        Some(Employment(
          employerName = npsEmployment.employerName,
          payeReference = npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber,
          taxablePayTotal = Some(payment.taxablePayYTD),
          taxTotal = Some(payment.totalTaxYTD)
        ))
      }
    }
  }


  def buildEmployment(rtiEmploymentsOption:Option[List[RtiEmployment]],iabdsOption: Option[List[Iabd]], npsEmployment: NpsEmployment): Employment = {

    (rtiEmploymentsOption,iabdsOption) match {

      case (None,None) => Employment(
        employerName = npsEmployment.employerName,
        payeReference = npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber)
      case (Some(Nil),Some(Nil))  => Employment(
        employerName = npsEmployment.employerName,
        payeReference = npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber)
      case (Some(Nil) | None,Some(y))=> {
        Employment(
          employerName = npsEmployment.employerName,
          payeReference = npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber,
          companyBenefits = getCompanyBenefits(y))
      }
      case (Some(x),Some(Nil) | None)=>  {
        val rtiPaymentInfo =getRtiPayment(x)
        Employment(
        employerName = npsEmployment.employerName,
        payeReference = npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber,
        taxablePayTotal = rtiPaymentInfo._1,
        taxTotal = rtiPaymentInfo._2)
      }
      case (Some(x),Some(y)) =>  {
        val rtiPaymentInfo =getRtiPayment(x)
        Employment(
          employerName = npsEmployment.employerName,
          payeReference = npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber,
          taxablePayTotal = rtiPaymentInfo._1,
          taxTotal = rtiPaymentInfo._2,
          companyBenefits = getCompanyBenefits(y))
      }
      case _ => Employment(
        employerName = npsEmployment.employerName,
        payeReference = npsEmployment.taxDistrictNumber + "/" + npsEmployment.payeNumber)
    }

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


  def getNpsEmployments(nino:Nino, taxYear:TaxYear)(implicit hc: HeaderCarrier): Future[Either[HttpResponse ,List[NpsEmployment]]] = {
    npsConnector.getEmployments(nino,taxYear.currentYear).map{
      response => {
        response.status match {
          case OK => {
            Right(response.json.as[List[NpsEmployment]])
          }
          case _ =>  Left(response)
        }
      }
    }
  }

  def getRtiEmployments(nino:Nino, taxYear:TaxYear)(implicit hc: HeaderCarrier): Future[Either[HttpResponse,RtiData]] = {
    rtiConnector.getRTIEmployments(nino,taxYear).map{
      response => {
        response.status match {
          case Status.OK => {
            Right(response.json.as[RtiData])
          }
          case _ =>  Left(response)
        }
      }
    }
  }


  def getNpsIabds(nino:Nino, taxYear:TaxYear)(implicit hc: HeaderCarrier): Future[Either[HttpResponse ,List[Iabd]]] = {
    npsConnector.getIabds(nino,taxYear.currentYear).map{
      response => {
        response.status match {
          case OK => {
            Right(response.json.as[List[Iabd]])
          }
          case _ =>  Left(response)
        }
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
    iabds.map {
      iabd => CompanyBenefit(typeDescription =
        iabd.typeDescription.fold{
          Logger.warn("Iabds Description is blank")
          ""}(x=>x),
        amount = iabd.grossAmount.fold{
          Logger.warn("Iabds grossAmount is blank")
          BigDecimal(0)
        }(x=>x))
    }
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
        }(x=>x))
    }
  }




}
