/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.model.nps

import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsSuccess, JsValue, Json, OWrites, Reads}

case class HipError(
  reason: String,
  code: String
) {
  override val toString: String = s"$code : $reason"
}
object HipError {
  implicit val reader: Reads[HipError]   = (js: JsValue) =>
    for {
      reason    <- (js \ "reason").validate[String]
      code      <- (js \ "code").validateOpt[String]
      errorType <- (js \ "type").validateOpt[String]
    } yield HipError(reason, code.getOrElse(errorType.getOrElse("")))
  implicit val writer: OWrites[HipError] = Json.writes[HipError]
}

case class HipErrors(
  errors: List[HipError]
) {
  override val toString: String = errors.mkString(" , ")
}

object HipErrors {
  implicit val reader: Reads[HipErrors]   = (js: JsValue) => {
    val json: JsValue =
      if (js.toString().contains("response")) js.apply("response")
      else js

    val errors: List[HipError] =
      if (json.toString().contains("failures")) (json \ "failures").as[List[HipError]]
      else List.empty :+ json.as[HipError]
    /* val key: Option[String] =
      if (js.toString().contains("response")) Some("response\\failures")
      else Some("failures").filter(js.toString().contains)

    errors =
      if (key.isEmpty) errors :+ js.as[HipError]
      else (js \ key.get).as[HipErrors].errors
     */
    JsSuccess(HipErrors(errors))
  }
  implicit val writer: OWrites[HipErrors] = Json.writes[HipErrors]

}
