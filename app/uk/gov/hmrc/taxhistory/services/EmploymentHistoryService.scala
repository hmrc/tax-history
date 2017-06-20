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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.EmploymentsConnector
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.model.taxhistory.Employment
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

object EmploymentHistoryService extends EmploymentHistoryService

trait EmploymentHistoryService {
  def employmentsConnector : EmploymentsConnector = EmploymentsConnector
  def rtiConnector : RtiConnector = RtiConnector

  def getEmploymentHistory(nino:String, taxYear:TaxYear): List[Employment] = ???

  def getNpsEmployments(nino:String, taxYear:TaxYear): List[NpsEmployment] = ???

  def getRtiEmployments(nino:String, taxYear:TaxYear): Future[Either[List[RtiData],HttpResponse]] = {
    val responseFuture = rtiConnector.getRTI(Nino(nino),taxYear)
     responseFuture.map(response => response.status match {
       case Status.OK => Left(response.json.as[List[RtiData]])
       case _ =>  Right(response)
     })
  }
}
