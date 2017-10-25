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


import play.api.Logger
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.MicroserviceAuditConnector
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.api.{CompanyBenefit, Employment}
import uk.gov.hmrc.taxhistory.model.nps.{NpsEmployment, _}
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentHistoryServiceHelper
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EmploymentHistoryService extends EmploymentHistoryService {
  override def audit = new Audit(appName,MicroserviceAuditConnector)
}

trait EmploymentHistoryService extends EmploymentHistoryServiceHelper with Auditable {
  def npsConnector : NpsConnector = NpsConnector
  def rtiConnector : RtiConnector = RtiConnector
  def cacheService : TaxHistoryCacheService = TaxHistoryCacheService


  def getEmployments(nino:String, taxYear:Int)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    implicit val validatedNino:Nino = Nino(nino)
    implicit val validatedTaxYear:TaxYear = TaxYear(taxYear)
    getFromCache.map(js => {
      Logger.warn("Returning js result from getEmployments")

      val extractEmployments = js.map(json =>
        json.\("employments").getOrElse(Json.arr())
      )

      extractEmployments match {
        case Some(emp) if emp.equals(Json.arr()) => HttpResponse(Status.NOT_FOUND, extractEmployments)
        case Some(emp) => HttpResponse(Status.OK, Some(enrichEmploymentsJsonWithGeneratedUrls(emp,taxYear=taxYear)))
      }
    })
  }

  def getEmployment(nino:String, taxYear:Int,employmentId:String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    implicit val validatedNino:Nino = Nino(nino)
    implicit val validatedTaxYear:TaxYear = TaxYear(taxYear)
    getFromCache.map(js => {
      Logger.warn("Returning js result of a getEmployment")
      js match {
        case Some(jsValue) =>
          (jsValue \ "employments").as[List[Employment]].find(_.employmentId.toString==employmentId) match {
            case Some(x) => HttpResponse(Status.OK,Some(Json.toJson(x.enrichWithURIs(taxYear))))
            case _ => {
              Logger.warn("Cache has expired from mongo")
              HttpResponse(Status.NOT_FOUND)
            }

          }
        case _ => HttpResponse(Status.NOT_FOUND)
      }
    })
  }


  def getFromCache(implicit validatedNino: Nino, validatedTaxYear: TaxYear, headerCarrier: HeaderCarrier) = {
    cacheService.getFromCacheOrElse {
      retrieveEmploymentsDirectFromSource(validatedNino, validatedTaxYear).map(h => {
        Logger.warn("Refresh cached data")
        h.json
      })
    }
  }

  def getAllowances(nino:String, taxYear:Int)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    implicit val validatedNino = Nino(nino)
    implicit val validatedTaxYear = TaxYear(taxYear)
    getFromCache.map(js => {
      Logger.warn("Returning js result from getAllowances")

      val extractAllowances = js.map(json =>
        json.\("allowances").getOrElse(Json.arr())
      )

      extractAllowances match {
        case Some(emp) if emp.equals(Json.arr()) => HttpResponse(Status.NOT_FOUND, extractAllowances)
        case _ => HttpResponse(Status.OK, extractAllowances)
      }
    })
  }


  def getPayAndTax(nino:String, taxYear:Int, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    implicit val validatedNino = Nino(nino)
    implicit val validatedTaxYear = TaxYear(taxYear)
    getFromCache.map(js => {
      Logger.warn("Returning js result from getEmployments")
      val extractPayAndTax = js.map(json =>
        (json \ "payAndTax"\ employmentId).getOrElse(Json.obj())
      )
      extractPayAndTax match {
        case Some(emp) if emp.equals(Json.obj()) => HttpResponse(Status.NOT_FOUND, extractPayAndTax)
        case _ => HttpResponse(Status.OK, extractPayAndTax)
      }
    })
  }

  def getCompanyBenefits(nino:String, taxYear:Int, employmentId:String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    implicit val validatedNino = Nino(nino)
    implicit val validatedTaxYear = TaxYear(taxYear)
    getFromCache.map(js => {
      Logger.warn("Returning js result from getCompanyBenefits")
      val extractCompanyBenefits = js.map(json =>
        (json \ "benefits"\ employmentId).getOrElse(Json.obj())
      )
      extractCompanyBenefits match {
        case Some(comBen) if comBen.equals(Json.obj()) => HttpResponse(Status.NOT_FOUND, extractCompanyBenefits)
        case _ => HttpResponse(Status.OK, extractCompanyBenefits)
      }
    })
  }

  def retrieveEmploymentsDirectFromSource(validatedNino:Nino,validatedTaxYear:TaxYear)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] ={
    val x = for {
      npsEmploymentsFuture <- getNpsEmployments(validatedNino, validatedTaxYear)
    }
      yield {
        npsEmploymentsFuture match {
          case Left(httpResponse) =>Future.successful(httpResponse)
          case Right(Nil) => Future.successful(HttpResponse(Status.NOT_FOUND, Some(Json.parse("[]"))))
          case Right(npsEmploymentList) => {
            mergeAndRetrieveEmployments(validatedNino,validatedTaxYear)(npsEmploymentList)
          }
        }
      }
    x.flatMap(identity)
  }

  def mergeAndRetrieveEmployments(nino: Nino, taxYear: TaxYear)(npsEmployments: List[NpsEmployment])
                                 (implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    for {
      iabdsF <- getNpsIabds(nino,taxYear)
      rtiF <- getRtiEmployments(nino,taxYear)
    }yield {
      combineResult(iabdsF,rtiF)(npsEmployments)
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
          case NOT_FOUND => {
            Logger.warn("NPS employments responded with not found")
            Right(Nil)
          }
          case _ => {
            Logger.warn("Non 200 response code from nps employment api.")
            Left(response)
          }
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
          case _ =>  {
            Logger.warn("Non 200 response code from rti employment api.")
            Left(response)
          }
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
          case _ =>  {
            Logger.warn("Non 200 response code from nps iabd api.")
            Left(response)
          }
        }
      }
    }
  }
}
