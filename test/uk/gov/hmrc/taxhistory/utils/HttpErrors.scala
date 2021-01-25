/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.utils

import play.api.http.Status._
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, UpstreamErrorResponse}

object HttpErrors {
  val toCheck: List[(Exception, Int)] = List(
    (new NotFoundException(""), NOT_FOUND),
    (new BadRequestException(""), BAD_REQUEST),
    (UpstreamErrorResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE), SERVICE_UNAVAILABLE),
    (UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR), INTERNAL_SERVER_ERROR)
  )
}
