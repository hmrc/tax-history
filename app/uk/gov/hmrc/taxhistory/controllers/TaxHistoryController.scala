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
import uk.gov.hmrc.taxhistory.{GenericHttpError, NotFound, TaxHistoryException}
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger

import scala.concurrent.{ExecutionContext, Future}

trait TaxHistoryController extends AuthController with TaxHistoryLogger {
  def toResult[A : Writes](fa: Future[A])(implicit ec: ExecutionContext): Future[Result] = {
    fa.map(value => Ok(Json.toJson(value))).recover {
      case TaxHistoryException(uk.gov.hmrc.taxhistory.NotFound(itemType, id), originator) =>
        val itemTypeStr = itemType.getSimpleName
        log(originator, s"Not found: item of type $itemTypeStr with id $id")
        NotFound // TODO add details to the result?
      case TaxHistoryException(uk.gov.hmrc.taxhistory.ServiceUnavailable, originator) =>
        log(originator, s"Service Unavailable")
        ServiceUnavailable // TODO add details to the result?
      case TaxHistoryException(uk.gov.hmrc.taxhistory.InternalServerError, originator) =>
        log(originator, "Internal Server Error")
        InternalServerError // TODO add details to the result?
      case TaxHistoryException(uk.gov.hmrc.taxhistory.BadRequest, originator) =>
        log(originator, "Bad Request")
        BadRequest // TODO add details to the result?
      case TaxHistoryException(GenericHttpError(status, response), originator) =>
        log(originator, "HTTP error " + status)
        Status(status)
      case TaxHistoryException(error, originator) =>
        log(originator, "Error: " + error)
        InternalServerError
      case _ =>
        logger.warn("Unknown error")
        InternalServerError
    }
  }

  def log(originator: Option[String], message: String) = {
    originator match {
      case Some(org) => logger.warn(s"$org returned: $message")
      case None      => logger.warn(message)
    }
  }
}
