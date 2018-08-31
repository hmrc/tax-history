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
import com.google.inject.name.Names.named
import com.kenshoo.play.metrics.{Metrics, MetricsImpl}
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{CoreGet, CorePost, HttpGet, HttpPost}
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.LoadAuditingConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.taxhistory._
import uk.gov.hmrc.taxhistory.metrics.TaxHistoryMetrics
import uk.gov.hmrc.taxhistory.services.{PayeCacheService, TaxHistoryMongoCacheService}

class Module(val environment: Environment, val configuration: Configuration) extends AbstractModule with ServicesConfig {

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration: Configuration = configuration

  def configure() = {

    bind(classOf[HttpGet]).to(classOf[WSHttpImpl])
    bind(classOf[HttpPost]).to(classOf[WSHttpImpl])
    bind(classOf[HttpClient]).to(classOf[WSHttpImpl])
    bind(classOf[AuditingConfig]).toProvider(new AuditingConfigProvider("auditing"))
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])
    bind(classOf[Audit]).to(classOf[MicroserviceAudit])
    bind(classOf[PayeCacheService]).to(classOf[TaxHistoryMongoCacheService])

    bindConfigInt("mongodb.cache.expire.seconds", default = Some(60 * 30))
    bindConfigString("mongodb.name")
    bindConfigString("microservice.services.nps-hod.originatorId",       default = Some("HMRC_GDS"))
    bindConfigString("microservice.services.rti-hod.originatorId",       default = Some("local"))
    bindConfigString("microservice.services.rti-hod.env",                default = Some("local"))
    bindConfigString("microservice.services.rti-hod.authorizationToken", default = Some("local"))
    bindConfigBoolean("featureFlags.currentYearFlag")
    bindConfigBoolean("featureFlags.statePensionFlag")
    bindConfigBoolean("featureFlags.jobSeekersAllowanceFlag")

    bindConfigProperty("des.authorizationToken")
    bindConfigProperty("des.env")

    bind(classOf[String]).annotatedWith(Names.named("rti-hod-base-url")).toProvider(provide(baseUrl("rti-hod")))
    bind(classOf[String]).annotatedWith(Names.named("nps-hod-base-url")).toProvider(provide(baseUrl("nps-hod")))
    bind(classOf[String]).annotatedWith(Names.named("des-base-url")).toProvider(provide(baseUrl("des")))

    bind(classOf[MongoDbConnection]).toProvider(provide(new MongoDbConnection {}))

    bind(classOf[String]).annotatedWith(Names.named("appName")).toProvider(AppNameProvider)
  }

  private def bindConfigProperty(propertyName: String) =
    bind(classOf[String]).annotatedWith(named(s"$propertyName")).toProvider(new ConfigPropertyProvider(propertyName))

  private class ConfigPropertyProvider(propertyName: String) extends Provider[String] {
    override lazy val get = getConfString(propertyName, throw new RuntimeException(s"No configuration value found for '$propertyName'"))
  }

  private def provide[A](value: => A): Provider[A] = new Provider[A] {
    def get(): A = value
  }

  private object AppNameProvider extends Provider[String] {
    def get(): String = AppName(configuration).appName
  }

  private class AuditingConfigProvider(key: String) extends Provider[AuditingConfig] {
    def get(): AuditingConfig = LoadAuditingConfig(configuration, environment.mode, key)
  }

  private def bindConfigString(propertyName: String, default: Option[String] = None) =
    bind(classOf[String]).annotatedWith(Names.named(s"$propertyName")).toProvider(ConfigStringProvider(propertyName, default))

  private case class ConfigStringProvider(propertyName: String, default: Option[String] = None) extends Provider[String] {
    lazy val get: String =
      configuration.getString(propertyName).orElse(default).getOrElse(throw new RuntimeException(s"No configuration value found for '$propertyName'"))
  }

  private def bindConfigBoolean(propertyName: String, default: Boolean = false) =
    bind(classOf[Boolean]).annotatedWith(Names.named(s"$propertyName")).toProvider(ConfigBooleanProvider(propertyName, Some(default)))

  private case class ConfigBooleanProvider(propertyName: String, default: Option[Boolean] = None) extends Provider[Boolean] {
    lazy val get: Boolean =
      configuration.getBoolean(propertyName).orElse(default).getOrElse(throw new RuntimeException(s"No configuration value found for '$propertyName'"))
  }

  private def bindConfigInt(propertyName: String, default: Option[Int] = None) =
    bind(classOf[Int]).annotatedWith(Names.named(s"$propertyName")).toProvider(ConfigIntProvider(propertyName, default))

  private case class ConfigIntProvider(propertyName: String, default: Option[Int] = None) extends Provider[Int] {
    lazy val get: Int =
      configuration.getInt(propertyName).orElse(default).getOrElse(throw new RuntimeException(s"No configuration value found for '$propertyName'"))
  }
}