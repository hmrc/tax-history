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
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment, RtiPayment}
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.nps.{NpsEmployment, _}
import uk.gov.hmrc.time.TaxYear
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.taxhistory.MicroserviceAuditConnector
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.model.taxhistory._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

object EmploymentHistoryService extends EmploymentHistoryService {
  override def audit = new Audit(appName,MicroserviceAuditConnector)
}

trait EmploymentHistoryService extends Auditable{
  def npsConnector : NpsConnector = NpsConnector
  def rtiConnector : RtiConnector = RtiConnector
  def cacheService : TaxHistoryCacheService = TaxHistoryCacheService

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

    val payAsYouEarnDetails = PayAsYouEarnDetails(employments = employments,allowances = allowances)

    HttpResponse(Status.OK,Some(Json.toJson(payAsYouEarnDetails)))
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
          companyBenefits = getCompanyBenefits(y),
          startDate = npsEmployment.startDate,
          endDate = npsEmployment.endDate)
      }
      case (Some(x), Some(Nil) | None) => {
        val rtiPaymentInfo = getRtiPayment(x)
        Employment(
          employerName = npsEmployment.employerName,
          payeReference = getPayeRef(npsEmployment),
          taxablePayTotal = rtiPaymentInfo._1,
          taxTotal = rtiPaymentInfo._2,
          earlierYearUpdates = convertRtiEYUToEYU(x),
          startDate = npsEmployment.startDate,
          endDate = npsEmployment.endDate
        )
      }
      case (Some(x), Some(y)) => {
        val rtiPaymentInfo = getRtiPayment(x)
        Employment(
          employerName = npsEmployment.employerName,
          payeReference = getPayeRef(npsEmployment),
          taxablePayTotal = rtiPaymentInfo._1,
          taxTotal = rtiPaymentInfo._2,
          earlierYearUpdates = convertRtiEYUToEYU(x),
          companyBenefits = getCompanyBenefits(y),
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


  def getNpsEmployments(nino:Nino, taxYear:TaxYear)(implicit hc: HeaderCarrier): Future[Either[HttpResponse ,List[NpsEmployment]]] = {
    npsConnector.getEmployments(nino,taxYear.currentYear).map{
      response => {
        response.status match {
          case OK => {
            val employments = response.json.as[List[NpsEmployment]].filterNot(x => x.receivingJobSeekersAllowance || x.otherIncomeSourceIndicator)
            Right(employments)
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




}
