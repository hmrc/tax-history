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

package uk.gov.hmrc.taxhistory

import javax.inject.{Inject, Named}

import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.microservice.config.LoadAuditingConfig


trait Hooks extends HttpHooks with HttpAuditing {
  val hooks = Seq(AuditingHook)
}

trait WSHttp extends HttpGet with WSGet
  with HttpPut with WSPut
  with HttpPost with WSPost
  with HttpDelete with WSDelete
  with HttpPatch with WSPatch
  with Hooks with AppName

class WSHttpImpl @Inject()(val auditConnector: AuditConnector) extends HttpGet with WSGet
  with HttpPut with WSPut
  with HttpPost with WSPost
  with HttpDelete with WSDelete
  with HttpPatch with WSPatch
  with Hooks with AppName

class MicroserviceAudit @Inject()(@Named("appName") val applicationName: String,
                                  val auditConnector: AuditConnector) extends Audit(applicationName, auditConnector)

class MicroserviceAuditConnector @Inject()(val auditingConfig: AuditingConfig) extends AuditConnector with RunMode

class MicroserviceAuthConnector @Inject()(val auditConnector: AuditConnector) extends AuthConnector with ServicesConfig with WSHttp {
  val authBaseUrl = baseUrl("auth")
}

class TaxHistoryAuthConnector @Inject()(val http: CorePost) extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl = baseUrl("auth")
}
