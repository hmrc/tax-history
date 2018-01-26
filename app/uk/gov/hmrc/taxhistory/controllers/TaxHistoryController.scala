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

package uk.gov.hmrc.taxhistory.controllers

import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import uk.gov.hmrc.taxhistory.{HttpNotOk, TaxHistoryException}
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger

import scala.concurrent.{ExecutionContext, Future}

trait TaxHistoryController extends AuthController with TaxHistoryLogger {
  def toResult[A : Writes](fa: Future[A])(implicit ec: ExecutionContext): Future[Result] = {
    fa.map(value => Ok(Json.toJson(value))).recover {
      case TaxHistoryException(HttpNotOk(NOT_FOUND, response)) =>
        logger.warn("Page Not Found")
        NotFound
      case TaxHistoryException(HttpNotOk(BAD_REQUEST, response)) =>
        logger.warn(s"Bad Request: ${response.body}")
        BadRequest
      case TaxHistoryException(HttpNotOk(SERVICE_UNAVAILABLE, response)) =>
        logger.warn(s"Service Unavailable: ${response.body}")
        ServiceUnavailable
      case _ =>
        logger.warn(s"Internal Service Error")
        InternalServerError
    }
  }
}
