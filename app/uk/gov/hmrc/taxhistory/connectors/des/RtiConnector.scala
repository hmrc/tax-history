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

package uk.gov.hmrc.taxhistory.connectors.des
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.taxhistory.connectors.BaseConnector
import uk.gov.hmrc.taxhistory.WSHttp
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future
import uk.gov.hmrc.http._

object RtiConnector extends RtiConnector {

  // $COVERAGE-OFF$
  override val httpGet: CoreGet = WSHttp
  override val httpPost: CorePost = WSHttp

  lazy val serviceUrl: String = s"${baseUrl("rti-hod")}"
  lazy val authorization: String = "Bearer " + getConfString("rti-hod.authorizationToken","local")
  lazy val environment: String = getConfString("rti-hod.env","local")
  lazy val originatorId = getConfString("rti-hod.originatorId","local")
  // $COVERAGE-ON$
}

trait RtiConnector extends BaseConnector {

  def serviceUrl: String
  def authorization: String
  def environment: String

  def rtiBasicUrl(nino: Nino) = s"$serviceUrl/rti/individual/payments/nino/${withoutSuffix(nino)}"

  def rtiPathUrl(nino: Nino, path: String) = s"${rtiBasicUrl(nino)}/$path"


  def createHeader: HeaderCarrier = HeaderCarrier(extraHeaders =
    Seq("Environment" -> environment,
      "Authorization" -> authorization,
      "Gov-Uk-Originator-Id" -> originatorId))

  def getRTIEmployments(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val twoDigitYear = s"${taxYear.startYear % 100}-${taxYear.finishYear % 100}"
    val urlToRead = rtiPathUrl(nino, s"tax-year/${twoDigitYear}")
    implicit val hc: HeaderCarrier = createHeader
    getFromRTI(urlToRead)
  }

}
