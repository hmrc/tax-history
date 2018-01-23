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

import javax.inject.Provider

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{CoreGet, CorePost}
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.microservice.config.LoadAuditingConfig
import uk.gov.hmrc.taxhistory._

class Module(val environment: Environment, val configuration: Configuration) extends AbstractModule with ServicesConfig {

  def configure() = {

    bind(classOf[CoreGet]).to(classOf[WSHttpImpl])
    bind(classOf[CorePost]).to(classOf[WSHttpImpl])
    bind(classOf[AuditingConfig]).toProvider(new AuditingConfigProvider("auditing"))
    bind(classOf[AuditConnector]).to(classOf[MicroserviceAuditConnector])
    bind(classOf[AuthConnector]).to(classOf[TaxHistoryAuthConnector])

    bind(classOf[ServicesConfig]).toInstance(new ServicesConfig {})

    bindConfigInt("mongodb.cache.expire.seconds", default = Some(60 * 30))
    bindConfigString("mongodb.name")
    bindConfigString("microservice.services.nps-hod.originatorId",       default = Some("HMRC_GDS"))
    bindConfigString("microservice.services.nps-hod.path",               default = Some(""))
    bindConfigString("microservice.services.rti-hod.originatorId",       default = Some("local"))
    bindConfigString("microservice.services.rti-hod.env",                default = Some("local"))
    bindConfigString("microservice.services.rti-hod.authorizationToken", default = Some("local"))

    bind(classOf[String]).annotatedWith(Names.named("appName")).toProvider(AppNameProvider)

    bind(classOf[Audit]).to(classOf[MicroserviceAudit])
  }

  private object AppNameProvider extends Provider[String] {
    def get(): String = AppName.appName
  }

  private class AuditingConfigProvider(key: String) extends Provider[AuditingConfig] {
    def get(): AuditingConfig = LoadAuditingConfig(key)
  }

  private def bindConfigString(propertyName: String, default: Option[String] = None) =
    bind(classOf[String]).annotatedWith(Names.named(s"$propertyName")).toProvider(ConfigStringProvider(propertyName, default))

  private case class ConfigStringProvider(propertyName: String, default: Option[String] = None) extends Provider[String] {
//    lazy val get = getConfString(propertyName, default.getOrElse(throw new RuntimeException(s"No configuration value found for '$propertyName'")))
    lazy val get = configuration.getString(propertyName).orElse(default).getOrElse(throw new RuntimeException(s"No configuration value found for '$propertyName'"))
  }

  private def bindConfigInt(propertyName: String, default: Option[Int] = None) =
    bind(classOf[Int]).annotatedWith(Names.named(s"$propertyName")).toProvider(ConfigIntProvider(propertyName, default))

  private case class ConfigIntProvider(propertyName: String, default: Option[Int] = None) extends Provider[Int] {
//    lazy val get = getConfInt(propertyName, default.getOrElse(throw new RuntimeException(s"No configuration value found for '$propertyName'")))
    lazy val get = configuration.getInt(propertyName).orElse(default).getOrElse(throw new RuntimeException(s"No configuration value found for '$propertyName'"))
  }
}