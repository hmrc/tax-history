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

package uk.gov.hmrc.taxhistory.model.nps

import play.api.libs.json._

sealed trait EmploymentStatus

object EmploymentStatus {

  case object Live extends EmploymentStatus
  case object PotentiallyCeased extends EmploymentStatus
  case object Ceased extends EmploymentStatus
  case object Unknown extends EmploymentStatus
// remove unknown and add permanently ceased: 6
  case object PermanentlyCeased extends EmploymentStatus

  private val LiveCode              = 1
  private val PotentiallyCeasedCode = 2
  private val CeasedCode            = 3
  private val UnknownCode           =
    99 // Code 99, Unknown, is internal to tax-history, and is not an wider HMRC employment status
  private val PermanentlyCeasedCode = 6

  given jsonReads: Reads[EmploymentStatus] =
    (__ \ "employmentStatus").read(Reads.of[String].orElse(Reads.of[Int].map(x => s"$x"))).flatMap[EmploymentStatus] {
      case "1"                  => Reads(_ => JsSuccess(Live))
      case "2"                  => Reads(_ => JsSuccess(PotentiallyCeased))
      case "3"                  => Reads(_ => JsSuccess(Ceased))
      case "99"                 => Reads(_ => JsSuccess(Unknown))
      case "6"                  => Reads(_ => JsSuccess(PermanentlyCeased))
      case "Live"               => Reads(_ => JsSuccess(Live))
      case "Potentially Ceased" => Reads(_ => JsSuccess(PotentiallyCeased))
      case "Ceased"             => Reads(_ => JsSuccess(Ceased))
      case "Unknown"            => Reads(_ => JsSuccess(Unknown))
      case "Permanently Ceased" => Reads(_ => JsSuccess(PermanentlyCeased))
      case _                    => Reads(_ => JsError(JsPath \ s"employmentStatus", JsonValidationError("Invalid EmploymentStatus")))
    }

  given jsonWrites: Writes[EmploymentStatus] = Writes[EmploymentStatus] {
    case Live              => Json.obj("employmentStatus" -> LiveCode)
    case PotentiallyCeased => Json.obj("employmentStatus" -> PotentiallyCeasedCode)
    case Ceased            => Json.obj("employmentStatus" -> CeasedCode)
    case Unknown           => Json.obj("employmentStatus" -> UnknownCode)
    case PermanentlyCeased => Json.obj("employmentStatus" -> PermanentlyCeasedCode)
  }
}
