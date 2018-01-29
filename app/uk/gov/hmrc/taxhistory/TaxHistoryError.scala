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

package uk.gov.hmrc.taxhistory

import play.api.libs.json.JsResultException
import uk.gov.hmrc.http.HttpResponse

case class TaxHistoryException(error: TaxHistoryError, originator: Option[String] = None) extends Exception {
  override def getMessage: String = error.toString
  override def toString: String = s"TaxHistoryException($error)"
}

object TaxHistoryException {
  def notFound(itemType: Class[_], id: Any): TaxHistoryException = TaxHistoryException(NotFound(itemType, id))
  def serviceUnavailable: TaxHistoryException = TaxHistoryException(ServiceUnavailable)
  def internalServerError: TaxHistoryException = TaxHistoryException(InternalServerError)
  def badRequest: TaxHistoryException = TaxHistoryException(BadRequest)
}

sealed trait TaxHistoryError

final case class NotFound(itemType: Class[_], id: Any) extends TaxHistoryError
case object ServiceUnavailable extends TaxHistoryError
case object InternalServerError extends TaxHistoryError
case object BadRequest extends TaxHistoryError
final case class GenericHttpError(status: Int, response: HttpResponse) extends TaxHistoryError
final case class JsonParsingError(jsonResultException: JsResultException) extends TaxHistoryError
final case class UnknownError(throwable: Throwable) extends TaxHistoryError
