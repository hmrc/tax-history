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
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.taxhistory.utils.Logging

import scala.concurrent.{ExecutionContext, Future}

trait TaxHistoryController extends BaseController with Logging {
  def toResult[A : Writes](fa: Future[A])(implicit ec: ExecutionContext): Future[Result] = {
    fa.map(value => Ok(Json.toJson(value))).recover {
      case e400: BadRequestException =>
        logger.warn("Bad Request: " + e400.message)
        BadRequest
      case e404: NotFoundException =>
        logger.warn("404 Not found: " + e404.message)
        NotFound
      case e4xx: Upstream4xxResponse =>
        logger.warn(s"Service returned error ${e4xx.upstreamResponseCode}: ${e4xx.message}")
        Status(e4xx.upstreamResponseCode)
      case e5xx: Upstream5xxResponse if e5xx.upstreamResponseCode == INTERNAL_SERVER_ERROR =>
        logger.warn("Internal server error")
        InternalServerError
      case e5xx: Upstream5xxResponse if e5xx.upstreamResponseCode == SERVICE_UNAVAILABLE =>
        logger.warn("Service unavailable")
        ServiceUnavailable
      case e5xx: Upstream5xxResponse =>
        logger.warn(s"Service returned error ${e5xx.upstreamResponseCode}: ${e5xx.message}")
        Status(e5xx.upstreamResponseCode)
      case ex @ _ =>
        logger.warn(s"Error: ${ex.toString} - ${ex.getMessage}")
        InternalServerError
    }
  }
}
