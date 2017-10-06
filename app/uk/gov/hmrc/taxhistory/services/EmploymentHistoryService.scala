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

import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.MicroserviceAuditConnector
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.api.Employment
import uk.gov.hmrc.taxhistory.model.nps.{NpsEmployment, _}
import uk.gov.hmrc.taxhistory.model.utils.PayAsYouEarnBuilder
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EmploymentHistoryService extends EmploymentHistoryService {
  override def audit = new Audit(appName,MicroserviceAuditConnector)
}

trait EmploymentHistoryService extends PayAsYouEarnBuilder{
  def npsConnector : NpsConnector = NpsConnector
  def rtiConnector : RtiConnector = RtiConnector
  def cacheService : TaxHistoryCacheService = TaxHistoryCacheService

  def getEmployments(nino:String, taxYear:Int)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val validatedNino = Nino(nino)
    val validatedTaxYear = TaxYear(taxYear)

    val x = for {
      cache <- cacheService.findById(validatedNino.formatted)//( for{newData <- retrieveEmploymentsDirectFromSource(validatedNino, validatedTaxYear)} yield {newData.json} )
    } yield {
      cache match {
        case Some(x:JsValue) => Future.successful(HttpResponse(Status.OK,Some(x)))
        case _ => {
          retrieveEmploymentsDirectFromSource(validatedNino,validatedTaxYear).map(a => {
            cacheService.createOrUpdate(validatedNino.formatted,validatedTaxYear.currentYear.toString,a.json)
            a
          })
        }
      }
    }
    x.flatMap(identity)
  }

  def retrieveEmploymentsDirectFromSource(validatedNino:Nino,validatedTaxYear:TaxYear)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] ={
    val x = for {
      npsEmploymentsFuture <- getNpsEmployments(validatedNino, validatedTaxYear)
    }
      yield {
        npsEmploymentsFuture match {
          case Left(httpResponse) =>Future.successful(httpResponse)
          case Right(Nil) => Future.successful(HttpResponse(Status.NOT_FOUND, Some(Json.parse("""{"Message":"Not Found"}"""))))
          case Right(npsEmploymentList) => {
            mergeAndRetrieveEmployments(validatedNino,validatedTaxYear)(npsEmploymentList)
          }
        }
      }
    x.flatMap(identity)
  }


  def mergeAndRetrieveEmployments(nino: Nino, taxYear: TaxYear)(npsEmployments: List[NpsEmployment])(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
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
}
