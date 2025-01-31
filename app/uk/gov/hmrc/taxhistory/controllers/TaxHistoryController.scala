/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.mvc._
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import play.api.Logging

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxHistoryController(cc: ControllerComponents) extends BackendController(cc) with Logging {

  def toResult[A: Writes](fa: Future[A])(implicit ec: ExecutionContext): Future[Result] =
    fa.map(value => Ok(Json.toJson(value))).recover {
      case e400: BadRequestException                                               =>
        logger.warn(s"[TaxHistoryController][toResult] Bad Request: ${e400.message}")
        BadRequest
      case e404: NotFoundException                                                 =>
        logger.info(s"[TaxHistoryController][toResult] 404 Not found: ${e404.message}")
        NotFound
      case UpstreamErrorResponse.Upstream4xxResponse(e4xx)                         =>
        logger.warn(s"[TaxHistoryController][toResult] Service returned error ${e4xx.statusCode}: ${e4xx.message}")
        Status(e4xx.statusCode)
      case e5xx: UpstreamErrorResponse if e5xx.statusCode == INTERNAL_SERVER_ERROR =>
        logger.error(s"[TaxHistoryController][toResult] Internal server error  ${e5xx.statusCode}: ${e5xx.message}")
        InternalServerError
      case e5xx: UpstreamErrorResponse if e5xx.statusCode == SERVICE_UNAVAILABLE   =>
        logger.error(s"[TaxHistoryController][toResult] Service unavailable  ${e5xx.statusCode}: ${e5xx.message}")
        ServiceUnavailable
      case e5xx: UpstreamErrorResponse                                             =>
        logger.error(s"[TaxHistoryController][toResult] Service returned error ${e5xx.statusCode}: ${e5xx.message}")
        Status(e5xx.statusCode)
      case ex @ _                                                                  =>
        logger.error(s"[TaxHistoryController][toResult] Error: ${ex.toString} - ${ex.getMessage}")
        InternalServerError
    }
}
