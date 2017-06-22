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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.EmploymentsConnector
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.time.TaxYear
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino

import uk.gov.hmrc.taxhistory.model.taxhistory.Employment


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EmploymentHistoryService extends EmploymentHistoryService

trait EmploymentHistoryService {
  def employmentsConnector : EmploymentsConnector = EmploymentsConnector
  def rtiConnector : RtiConnector = RtiConnector
  lazy val notFoundMessage = Json.parse("")

  def getEmploymentHistory(nino:String, taxYear:Int)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val validatedNino = Nino(nino)
    val validatedTaxYear = TaxYear(taxYear)

    val x = for {
                  npsEmploymentsFuture <- getNpsEmployments(validatedNino, validatedTaxYear)
                }
                yield {
                  npsEmploymentsFuture match {
                    case Left(httpResponse) =>Future.successful(httpResponse)
                    case Right(Nil) => Future.successful(HttpResponse(Status.NOT_FOUND, Some(notFoundMessage)))
                    case Right(npsEmploymentList) => {
                      handleNpsEmploymentList(validatedNino, validatedTaxYear)(npsEmploymentList, createEmploymentList)
                    }
                  }
                }
    x.flatMap(identity)
  }


  def handleNpsEmploymentList(nino:Nino,taxYear:TaxYear)
                             (npsEmploymentList:List[NpsEmployment],
                              mergeEmployments: (RtiData,List[NpsEmployment]) => List[Employment])
                             (implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    for(rtiDataFuture <- getRtiEmployments(nino, taxYear))
      yield {
        rtiDataFuture match {
          case Left(httpResponse) =>httpResponse
          case Right(rtiData) => {
            HttpResponse(Status.OK,
              Some(Json.toJson(mergeEmployments(rtiData,npsEmploymentList))))
          }
        }
      }
  }

  def createEmploymentList(rtiData:RtiData, npsEmployments: List[NpsEmployment]): List[Employment] = {
    npsEmployments.flatMap {
      npsEmployment => {
        rtiData.employments.filter(
          rtiEmployment => {
            rtiEmployment.sequenceNo == npsEmployment.sequenceNumber &&
            rtiEmployment.payeRef == npsEmployment.payeNumber &&
            rtiEmployment.officeNumber == npsEmployment.taxDistrictNumber
          }
        ) match {
            case Nil => None
            case start :: end => None //TODO mmmm???
            case matchingEmp :: Nil => {
              val payment = matchingEmp.payments.sorted.last //TODO deal with no payments
              Some(Employment(
                employerName = npsEmployment.employerName,
                payeReference = npsEmployment.payeNumber,
                taxablePayTotal = payment.taxablePayYTD,
                taxTotal = payment.totalTaxYTD
              ))
            }
        }
      }
    }
  }

  def getNpsEmployments(nino:Nino, taxYear:TaxYear)(implicit hc: HeaderCarrier): Future[Either[HttpResponse ,List[NpsEmployment]]] = {
    employmentsConnector.getEmployments(nino,taxYear.currentYear).map{
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
}
