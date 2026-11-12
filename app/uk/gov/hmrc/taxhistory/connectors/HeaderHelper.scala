/*
 * Copyright 2026 HM Revenue & Customs
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

import sttp.model.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.taxhistory.config.AppConfig

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.inject.{Inject, Singleton}

@Singleton
class HeaderHelper @Inject() (
                             val config: AppConfig
                             ) extends ConnectorCorrelationId {

  private lazy val clientId: String     = config.clientId
  private lazy val clientSecret: String = config.secret

  private lazy val authSecret: String =
    Base64.getEncoder
      .encodeToString(
        s"$clientId:$clientSecret".getBytes(StandardCharsets.UTF_8)
      )

  def createHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] =
    if (config.api5990) createHipHeaders else createDesHeaders

  private def createDesHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      "Environment"      -> config.rtiEnv,
      "Authorization"    -> s"Bearer ${config.rtiAuth}",
      CORRELATION_HEADER -> getCorrelationId(hc)
    )

  private def createHipHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    Seq(
      HeaderNames.Authorization -> s"Basic $authSecret",
      "correlationid" -> getCorrelationId(hc),
      "X-Receipt-Date" -> DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
      "X-Transmitting-System" -> "HIP",
      config.serviceOriginatorIdKey -> config.serviceOriginatorIDValue // TODO: may not be necessary. to confirm
    )
  }
}
