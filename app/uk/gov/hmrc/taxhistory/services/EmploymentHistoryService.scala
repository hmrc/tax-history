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
import uk.gov.hmrc.domain.Nino

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EmploymentHistoryService extends EmploymentHistoryService

trait EmploymentHistoryService {
  def employmentsConnector : EmploymentsConnector = EmploymentsConnector
  def rtiConnector : RtiConnector = RtiConnector

  def getEmploymentHistory(nino:String, taxYear:Int)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

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
