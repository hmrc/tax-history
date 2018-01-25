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


import javax.inject.Inject

import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.{HttpNotOk, TaxHistoryException}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.api.{Employment, IndividualTaxYear}
import uk.gov.hmrc.taxhistory.model.nps.{NpsEmployment, _}
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentHistoryServiceHelper
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class EmploymentHistoryService @Inject()(
                              val audit: Audit,
                              val npsConnector: NpsConnector,
                              val rtiConnector: RtiConnector,
                              val cacheService : TaxHistoryCacheService
                              ) extends EmploymentHistoryServiceHelper with Auditable with TaxHistoryLogger {

  def getEmployments(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    getFromCache(nino, taxYear).map { js =>
      logger.warn("Returning js result from getEmployments")

      val extractEmployments = js.map(json =>
        json.\("employments").getOrElse(Json.arr())
      )

      extractEmployments match {
        case Some(emp) if emp.equals(Json.arr()) => HttpResponse(Status.NOT_FOUND, extractEmployments)
        case Some(emp) => HttpResponse(Status.OK, Some(enrichEmploymentsJsonWithGeneratedUrls(emp, taxYear=taxYear.startYear)))
      }
    }
  }

  def getEmployment(nino: Nino, taxYear: TaxYear, employmentId:String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    getFromCache(nino, taxYear).map { js =>
      logger.warn("Returning js result of a getEmployment")
      js match {
        case Some(jsValue) =>
          (jsValue \ "employments").as[List[Employment]].find(_.employmentId.toString == employmentId) match {
            case Some(x) =>
              HttpResponse(Status.OK,Some(Json.toJson(x.enrichWithURIs(taxYear.startYear))))
            case _ =>
              logger.warn("Cache has expired from mongo")
              HttpResponse(Status.NOT_FOUND)
          }
        case _ => HttpResponse(Status.NOT_FOUND)
      }
    }
  }


  def getFromCache(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[Option[JsValue]] = {
    cacheService.getOrElseInsert(nino, taxYear) {
      retrieveEmploymentsDirectFromSource(nino, taxYear).map { h =>
        logger.warn("Refresh cached data")
        h.json
      }
    }
  }

  def getAllowances(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    getFromCache(nino, taxYear).map(js => {
      logger.warn("Returning js result from getAllowances")

      val extractAllowances = js.map(json =>
        json.\("allowances").getOrElse(Json.arr())
      )

      extractAllowances match {
        case Some(emp) if emp.equals(Json.arr()) => HttpResponse(Status.NOT_FOUND, extractAllowances)
        case _ => HttpResponse(Status.OK, extractAllowances)
      }
    })
  }


  def getPayAndTax(nino: Nino, taxYear: TaxYear, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    getFromCache(nino, taxYear).map { js =>
      logger.warn("Returning js result from getEmployments")
      val extractPayAndTax = js.map(json =>
        (json \ "payAndTax" \ employmentId).getOrElse(Json.obj())
      )
      extractPayAndTax match {
        case Some(emp) if emp.equals(Json.obj()) => HttpResponse(Status.NOT_FOUND, extractPayAndTax)
        case _ => HttpResponse(Status.OK, extractPayAndTax)
      }
    }
  }

  def getTaxAccount(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    getFromCache(nino, taxYear).map(js => {
      logger.warn("Returning js result from getTaxAccount")

      val extractTaxAccount = js.map(json =>
        json.\("taxAccount").getOrElse(Json.obj())
      )

      extractTaxAccount match {
        case Some(emp) if emp.equals(Json.obj()) => HttpResponse(Status.NOT_FOUND, extractTaxAccount)
        case _ => HttpResponse(Status.OK, extractTaxAccount)
      }
    })
  }

  def getTaxYears(nino: Nino) (implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {

    val taxYearList = List(TaxYear.current.back(1),
                           TaxYear.current.back(2),
                           TaxYear.current.back(3),
                           TaxYear.current.back(4))

    val taxYears = taxYearList.map(year => IndividualTaxYear(year = year.startYear,
                                                             allowancesURI = s"/${year.startYear}/allowances",
                                                             employmentsURI = s"/${year.startYear}/employments",
                                                             taxAccountURI = s"/${year.startYear}/tax-account"))

    Future.successful(HttpResponse(Status.OK, Some(Json.toJson(taxYears))))
  }

  def getCompanyBenefits(nino: Nino, taxYear: TaxYear, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    getFromCache(nino, taxYear).map(js => {
      logger.warn("Returning js result from getCompanyBenefits")
      val extractCompanyBenefits = js.map(json =>
        (json \ "benefits" \ employmentId).getOrElse(Json.obj())
      )
      extractCompanyBenefits match {
        case Some(comBen) if comBen.equals(Json.obj()) => HttpResponse(Status.NOT_FOUND, extractCompanyBenefits)
        case _ => HttpResponse(Status.OK, extractCompanyBenefits)
      }
    })
  }

  def retrieveEmploymentsDirectFromSource(validatedNino: Nino, validatedTaxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {

    for {
      employments <- getNpsEmployments(validatedNino, validatedTaxYear)
      result      <-
        if (employments.isEmpty) {
          Future.failed(TaxHistoryException(HttpNotOk(NOT_FOUND, HttpResponse(NOT_FOUND))))
          // TODO this is to preserve existing logic. To review
        } else {
          mergeAndRetrieveEmployments(validatedNino, validatedTaxYear)(employments)
        }
    } yield result
  }

  def mergeAndRetrieveEmployments(nino: Nino, taxYear: TaxYear)(npsEmployments: List[NpsEmployment])
                                 (implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    for {
      iabdsF  <- getNpsIabds(nino,taxYear)
      rtiF    <- getRtiEmployments(nino,taxYear).map(Right(_)).recover { case TaxHistoryException(HttpNotOk(_, response)) => Left(response) } // TODO this is done to preserve existing logic. Logic of 'combineResult' to be reviewed!
      taxAccF <- getNpsTaxAccount(nino,taxYear)
    } yield {
      combineResult(iabdsF,rtiF,taxAccF)(npsEmployments)
    }
  }

  def getNpsEmployments(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[List[NpsEmployment]] = {
    npsConnector.getEmployments(nino, taxYear.currentYear).map { employments =>
      employments.filterNot(x => x.receivingJobSeekersAllowance || x.otherIncomeSourceIndicator)
    }.recover {
      case TaxHistoryException(HttpNotOk(NOT_FOUND, _)) => Nil // In case of a 404 don't fail but instead return an empty list
    }
  }

  def getRtiEmployments(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[RtiData] = {
    rtiConnector.getRTIEmployments(nino, taxYear)
  }

  def getNpsIabds(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[HttpResponse ,List[Iabd]]] = {
    npsConnector.getIabds(nino, taxYear.currentYear).map{
      response => {
        response.status match {
          case OK => {
            Right(response.json.as[List[Iabd]])
          }
          case _ =>  {
            logger.warn("Non 200 response code from nps iabd api.")
            Left(response)
          }
        }
      }
    }
  }

  def getNpsTaxAccount(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[HttpResponse, Option[NpsTaxAccount]]] = {

    if (taxYear.startYear != TaxYear.current.previous.startYear) {
      Future.successful(Right(None))
    }
    else {
      npsConnector.getTaxAccount(nino, taxYear.currentYear).map {
        response => {
          response.status match {
            case OK =>
              Right(Some(response.json.as[NpsTaxAccount]))
            case _ =>
              logger.warn("Non 200 response code from nps iabd api.")
              Left(response)
          }
        }
      }
    }
  }

}
