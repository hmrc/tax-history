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

package uk.gov.hmrc.taxhistory.connectors.nps

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
import uk.gov.hmrc.tai.connectors.BaseConnector
import uk.gov.hmrc.taxhistory.WSHttp

import scala.concurrent.Future

/**
  * Created by shailesh on 18/06/17.
  */
 trait EmploymentsConnector extends BaseConnector {



  def serviceUrl: String


  def npsBaseUrl(nino: Nino) = s"$serviceUrl/person/$nino"
  def npsPathUrl(nino: Nino, path: String) = s"${npsBaseUrl(nino)}/$path"




  def getEmployments(nino: Nino, year: Int)
                    (implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val urlToRead = npsPathUrl(nino, s"employment/$year")
    httpGet.GET[HttpResponse](urlToRead)
  }


}


object EmploymentsConnector extends EmploymentsConnector {

  override val httpGet: HttpGet = WSHttp
  override val httpPost: HttpPost = WSHttp

  lazy val serviceUrl: String = s"${baseUrl("nps-hod")}"
  lazy val originatorId = getConfString(s"$services.nps-hod.originatorId","local")


}