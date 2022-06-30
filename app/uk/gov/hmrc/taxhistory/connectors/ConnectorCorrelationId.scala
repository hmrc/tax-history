/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.utils.Logging

import java.util.UUID.randomUUID
import scala.util.matching.Regex

protected trait ConnectorCorrelationId extends Logging {
  protected val CORRELATION_HEADER = "CorrelationId"
  private val CorrelationIdPattern: Regex = """.*([A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}).*""".r
  private val twentyFour = 24

  def generateNewUUID : String = randomUUID.toString

  def getCorrelationId(hc: HeaderCarrier): String = {
    hc.requestId match {
      case Some(requestId) =>
        requestId.value match {
          case CorrelationIdPattern(prefix) => prefix + "-" + generateNewUUID.substring(twentyFour)
          case _ => generateNewUUID
        }
      case _ => generateNewUUID
    }
  }
}
