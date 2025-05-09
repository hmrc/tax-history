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

package uk.gov.hmrc.taxhistory.connectors

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID.randomUUID
import scala.util.matching.Regex

protected trait ConnectorCorrelationId extends Logging {
  protected val CORRELATION_HEADER               = "CorrelationId"
  protected val HIP_CORRELATION_HEADER: String   = "correlationId"
  protected val HIP_AUTHORIZATION_HEADER: String = "Authorization"
  private val CorrelationIdPattern: Regex        = """.*([A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}).*""".r
  private val twentyFour                         = 24

  def generateNewUUID: String = randomUUID.toString

  def getCorrelationId(hc: HeaderCarrier): String =
    hc.requestId match {
      case Some(requestId) =>
        requestId.value match {
          case CorrelationIdPattern(prefix) => prefix + "-" + generateNewUUID.substring(twentyFour)
          case _                            => generateNewUUID
        }
      case _               => generateNewUUID
    }

  def getHIPCorrelationId(hc: HeaderCarrier): String =
    try {
      val candidateUUID = hc.requestId
        .map(_.value)
        .flatMap(rid => CorrelationIdPattern.findFirstMatchIn(rid).map(_.group(1)))
        .map(prefix => s"$prefix-${generateNewUUID.takeRight(12)}")
        .getOrElse(generateNewUUID)
      java.util.UUID.fromString(candidateUUID)
      candidateUUID
    } catch {
      case _: Exception => generateNewUUID
    }
}
