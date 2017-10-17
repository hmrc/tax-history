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

import org.joda.time.LocalDate
import play.api.Logger
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.MicroserviceAuditConnector
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.api.{Allowance, CompanyBenefit, EarlierYearUpdate, PayAndTax}
import uk.gov.hmrc.taxhistory.model.nps.{NpsEmployment, _}
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentHistoryServiceHelper
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EmploymentHistoryService extends EmploymentHistoryService {
  override def audit = new Audit(appName,MicroserviceAuditConnector)
}

trait EmploymentHistoryService extends EmploymentHistoryServiceHelper {
  def npsConnector : NpsConnector = NpsConnector
  def rtiConnector : RtiConnector = RtiConnector
  def cacheService : TaxHistoryCacheService = TaxHistoryCacheService

  def getEmployments(nino:String, taxYear:Int)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val validatedNino = Nino(nino)
    val validatedTaxYear = TaxYear(taxYear)

    cacheService.getFromCache(validatedNino.nino,validatedTaxYear){
      retrieveEmploymentsDirectFromSource(validatedNino,validatedTaxYear).map(h => {
          Logger.warn("Refresh cached data")
          h.json
        })
    }.map(js => {
      Logger.warn("Returning js result from getEmployments")
      HttpResponse(Status.OK,js)
    })
  }

  def getAllowances(nino:String, taxYear:Int)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val allowances = List(
                      new Allowance(
                            iabdType = "FlatRateJobExpenses",
                            amount=BigDecimal(11)))
    //TODO remove mock stub allowances
    Future.successful(HttpResponse(Status.OK,Some(Json.toJson(allowances))))
  }

  def getPayAndTax(nino:String, taxYear:Int, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val eyu = List(EarlierYearUpdate(
      taxablePayEYU = BigDecimal(1200),
      taxEYU = BigDecimal(400),
      receivedDate = new LocalDate("2015-15-29")))
    val payAndTax = PayAndTax(
      taxablePayTotal = Some(BigDecimal(21000.21)),
      taxTotal = Some(BigDecimal(4000.04)),
      earlierYearUpdates = eyu)
    //TODO remove mock stub pay and tax
    Future.successful(HttpResponse(Status.OK, Some(Json.toJson(payAndTax))))

  }
  def getCompanyBenefits(nino:String, taxYear:Int, employmentId:String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    //TODO Remove hard coding for stub company benefits
    val benefits = List(new CompanyBenefit(iabdType = "CompanyCar", amount=BigDecimal(666)))
    Future.successful(HttpResponse(Status.OK,Some(Json.toJson(benefits))))

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
