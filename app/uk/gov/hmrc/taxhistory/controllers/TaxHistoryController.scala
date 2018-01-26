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
import uk.gov.hmrc.taxhistory.{HttpNotOk, NotFound, TaxHistoryException}
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger

import scala.concurrent.{ExecutionContext, Future}

trait TaxHistoryController extends AuthController with TaxHistoryLogger {
  def toResult[A : Writes](fa: Future[A])(implicit ec: ExecutionContext): Future[Result] = {
    fa.map(value => Ok(Json.toJson(value))).recover {
      case TaxHistoryException(uk.gov.hmrc.taxhistory.NotFound(itemType, id), originator) =>
        val itemTypeStr = itemType.getSimpleName
        originator match {
          case Some(org) => logger.warn(s"$org returned a Not Found response for item of type $itemTypeStr requested with parameters $id")
          case None      => logger.warn(s"Not found: item of type $itemTypeStr with id $id")
        }
        NotFound // TODO add details to the result?
      case TaxHistoryException(HttpNotOk(NOT_FOUND, response), originator) =>
        logger.warn("Page Not Found")
        NotFound
      case TaxHistoryException(HttpNotOk(BAD_REQUEST, response), originator) =>
        logger.warn(s"Bad Request: ${response.body}")
        BadRequest
      case TaxHistoryException(HttpNotOk(SERVICE_UNAVAILABLE, response), originator) =>
        logger.warn(s"Service Unavailable: ${response.body}")
        ServiceUnavailable
      case _ =>
        logger.warn(s"Internal Service Error")
        InternalServerError
    }
  }
}
