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

package uk.gov.hmrc.taxhistory.model.utils

import org.joda.time.LocalDate
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json._

import scala.util.matching.Regex

object JsonUtils {
  def mapFormat[K, V](keyLabel: String, valueLabel: String)
                     (implicit kf: Format[K], vf: Format[V]): Format[Map[K, V]] =
    new Format[Map[K, V]] {
      def writes(m: Map[K, V]): JsValue =
        JsArray(
          m.map { case (t, v) => Json.obj(
            keyLabel -> kf.writes(t),
            valueLabel -> vf.writes(v)
          )
          }.toSeq
        )

      def reads(jv: JsValue): JsResult[Map[K, V]] = jv match {
        case JsArray(b) => JsSuccess(
          b.map(x => (x \ keyLabel).as[K] -> (x \ valueLabel).as[V]).toMap)
        case x => JsError(s"Expected JsArray(...), found $x")
      }
    }

  lazy val rtiDateFormat: Format[LocalDate] = Format(
    new Reads[LocalDate]{
      val dateRegex: Regex = """^(\d\d\d\d)-(\d\d)-(\d\d)$""".r
      override def reads(json: JsValue): JsResult[LocalDate] = json match {
        case JsString(dateRegex(y, m, d)) =>
          JsSuccess(new LocalDate(y.toInt, m.toInt, d.toInt))
        case invalid => JsError(JsonValidationError(
          s"Invalid date format [yyyy-MM-dd]: $invalid"))
      }
    },
    new Writes[LocalDate]{
      val dateFormat: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      override def writes(date: LocalDate): JsValue =
        JsString(dateFormat.print(date))
    }
  )

  lazy val npsDateFormat: Format[LocalDate] = Format(
    new Reads[LocalDate]{
      val dateRegex: Regex = """^(\d\d)/(\d\d)/(\d\d\d\d)$""".r
      override def reads(json: JsValue): JsResult[LocalDate] = json match {
        case JsString(dateRegex(d, m, y)) =>
          JsSuccess(new LocalDate(y.toInt, m.toInt, d.toInt))
        case invalid => JsError(JsonValidationError(
          s"Invalid NPS date format [dd/MM/yyyy]: $invalid"))
      }
    },
    new Writes[LocalDate]{
      val dateFormat: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      override def writes(date: LocalDate): JsValue =
        JsString(dateFormat.print(date))
    }
  )
}
