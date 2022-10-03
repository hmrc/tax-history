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

package uk.gov.hmrc.taxhistory.config

import akka.actor.ActorSystem

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.taxhistory.utils.Retry

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.language.postfixOps

@Singleton
class AppConfig @Inject() (config: ServicesConfig) {

  lazy val appName: String = config.getString("appName")

  // Mongo Configuration
  lazy val mongoExpiry: Duration = Duration(config.getInt("mongodb.cache.expire.seconds"), TimeUnit.SECONDS)
  lazy val mongoName: String     = config.getString("mongodb.name")

  // Des Configuration
  lazy val desEnv: String     = config.getConfString("des.env", "local")
  lazy val desAuth: String    = config.getConfString("des.authorizationToken", "Local")
  lazy val desBaseUrl: String = config.baseUrl("des")

  // IF Configuration
  lazy val ifsEnv: String     = config.getConfString("ifs.env", "local")
  lazy val ifsAuth: String    = config.getConfString("ifs.authorizationToken", "Local")
  lazy val ifsBaseUrl: String = config.baseUrl("ifs")

  // Feature flags
  lazy val currentYearFlag: Boolean         = config.getBoolean("featureFlags.currentYearFlag")
  lazy val statePensionFlag: Boolean        = config.getBoolean("featureFlags.statePensionFlag")
  lazy val jobSeekersAllowanceFlag: Boolean = config.getBoolean("featureFlags.jobSeekersAllowanceFlag")
  lazy val ifsEnabledFlag: Boolean          = config.getBoolean("featureFlags.ifsEnabledFlag")

  // Citizen Details Configuration
  lazy val citizenDetailsBaseUrl: String = config.baseUrl("citizen-details")

  def newRetryInstance(name: String, actorSystem: ActorSystem): Retry = {
    val times    = config.getInt(s"microservice.services.$name.retry.times")
    val interval = getConfFiniteDuration(name, 500 millis)
    new Retry(times, interval, actorSystem)
  }

  private def getConfFiniteDuration(name: String, default: FiniteDuration): FiniteDuration = {
    val d = FiniteDuration(config.getInt(s"microservice.services.$name.retry.interval"), "ms")
    require(d.isFinite(), s"not a finite duration: microservice.services.$name.retry.interval")
    FiniteDuration(d.length, d.unit)
  }

}
