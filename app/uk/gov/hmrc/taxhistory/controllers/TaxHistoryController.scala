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

import play.api.mvc.Result
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger

trait TaxHistoryController extends AuthController with TaxHistoryLogger{
  def matchResponse(response: HttpResponse): Result = response.status match {
    case OK => Ok(response.body)
    case NOT_FOUND =>
      logger.warn("Page Not Found")
      NotFound
    case BAD_REQUEST =>
      logger.warn(s"Bad Request: ${response.body}")
      BadRequest(response.body)
    case SERVICE_UNAVAILABLE =>
      logger.warn(s"Service Unavailable: ${response.body}")
      ServiceUnavailable
    case _ =>
      logger.warn(s"Internal Service Error : ${response.body}")
      InternalServerError
  }
}
