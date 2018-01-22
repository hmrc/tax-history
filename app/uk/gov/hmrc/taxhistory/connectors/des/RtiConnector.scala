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

package uk.gov.hmrc.taxhistory.connectors.des

import javax.inject.{Inject, Named}

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.taxhistory.connectors.BaseConnector
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class RtiConnector @Inject()(val httpGet: CoreGet,
                             val httpPost: CorePost,
                             val audit: Audit,
                             @Named("service-url") val serviceUrl: String,
                             @Named("authorization") val authorization: String,
                             @Named("environment") val environment: String,
                             @Named("originator-id") val originatorId: String
                            ) extends BaseConnector {

  def rtiBasicUrl(nino: Nino) = s"$serviceUrl/rti/individual/payments/nino/${withoutSuffix(nino)}"

  def rtiPathUrl(nino: Nino, path: String) = s"${rtiBasicUrl(nino)}/$path"


  def createHeader: HeaderCarrier = HeaderCarrier(extraHeaders =
    Seq("Environment" -> environment,
      "Authorization" -> authorization,
      "Gov-Uk-Originator-Id" -> originatorId))

  def getRTIEmployments(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val twoDigitYear = s"${taxYear.startYear % 100}-${taxYear.finishYear % 100}"
    val urlToRead = rtiPathUrl(nino, s"tax-year/$twoDigitYear")
    implicit val hc: HeaderCarrier = createHeader
    getFromRTI(urlToRead)
  }
}