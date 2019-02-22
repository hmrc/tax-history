/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import com.google.inject.name.Names
import javax.inject.{Named, Provider}
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.LoadAuditingConfig
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient, HttpClient}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.taxhistory._
import uk.gov.hmrc.taxhistory.services.{PayeCacheService, TaxHistoryMongoCacheService}
import uk.gov.hmrc.taxhistory.utils.Retry

import scala.concurrent.duration._

class Module(val environment: Environment, val configuration: Configuration) extends AbstractModule with ServicesConfig {

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration: Configuration = configuration

  def configure() = {

    bind(classOf[HttpClient]).to(classOf[DefaultHttpClient])
    bind(classOf[AuditingConfig]).toProvider(new AuditingConfigProvider("auditing"))
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])
    bind(classOf[Audit]).to(classOf[MicroserviceAudit])
    bind(classOf[PayeCacheService]).to(classOf[TaxHistoryMongoCacheService])

    bindConfigInt("mongodb.cache.expire.seconds", default = Some(60 * 30))
    bindConfigString("mongodb.name")
    bindConfigString("microservice.services.nps-hod.originatorId",       default = Some("HMRC_GDS"))
    bindConfigString("microservice.services.des.env",                default = Some("local"))
    bindConfigString("microservice.services.des.authorizationToken", default = Some("local"))
    bindConfigBoolean("featureFlags.currentYearFlag")
    bindConfigBoolean("featureFlags.statePensionFlag")
    bindConfigBoolean("featureFlags.jobSeekersAllowanceFlag")

    bind(classOf[String]).annotatedWith(Names.named("nps-hod-base-url")).toProvider(provide(baseUrl("nps-hod")))
    bind(classOf[String]).annotatedWith(Names.named("des-base-url")).toProvider(provide(baseUrl("des")))
    bind(classOf[String]).annotatedWith(Names.named("citizen-details-base-url")).toProvider(provide(baseUrl("citizen-details")))

    bind(classOf[MongoDbConnection]).toProvider(provide(new MongoDbConnection {}))

    bind(classOf[String]).annotatedWith(Names.named("appName")).toProvider(AppNameProvider)
  }

  @Provides
  @Named("des") def providesRetryForDes(system: ActorSystem): Retry = newRetryInstance("des", system)

  @Provides
  @Named("nps-hod") def providesRetryForNps(system: ActorSystem): Retry = newRetryInstance("nps-hod", system)

  @Provides
  @Named("citizen-details") def providesRetryForCitizenDetails(system: ActorSystem): Retry = newRetryInstance("citizen-details", system)

  private def newRetryInstance(name: String, actorSystem: ActorSystem): Retry = {
    val times = getConfInt(s"$name.retry.times", 1)
    val interval = getConfFiniteDuration(s"$name.retry.interval", 500 millis)
    new Retry(times, interval, actorSystem)
  }

  private def provide[A](value: => A): Provider[A] = new Provider[A] {
    def get(): A = value
  }

  private def getConfFiniteDuration(key: String, default: FiniteDuration): FiniteDuration = {
    val d = getConfDuration(key, default)
    require(d.isFinite(), s"not a finite duration: $key")
    FiniteDuration(d.length, d.unit)
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
