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
            handleNpsEmploymentList(validatedNino, validatedTaxYear)(npsEmploymentList, createEmploymentList)
          }
        }
      }
    x.flatMap(identity)
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

  def createEmploymentList(rtiData:Option[RtiData], npsEmployments: List[NpsEmployment]): List[Employment] = {
    npsEmployments.flatMap {
      npsEmployment => {
        val f = rtiData.map(
          data =>
            data.employments.filter {
              rtiEmployment => {
                Logger.warn(s"Comparing rti-nps payRef [${rtiEmployment.payeRef}]:[${npsEmployment.payeNumber}]-[${rtiEmployment.officeNumber}]:[${npsEmployment.taxDistrictNumber}]")
                rtiEmployment.payeRef == npsEmployment.payeNumber &&
                  rtiEmployment.officeNumber == npsEmployment.taxDistrictNumber
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
