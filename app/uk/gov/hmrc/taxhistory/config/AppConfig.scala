/*
 * Copyright 2020 HM Revenue & Customs
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

import scala.concurrent.duration.{DurationInt, FiniteDuration}

@Singleton
class AppConfig @Inject()(config: ServicesConfig) {

  lazy val appName: String = config.getString("appName")
  lazy val mongoExpiry: Int = config.getInt("mongodb.cache.expire.seconds")
  lazy val mongoName: String = config.getString("mongodb.name")
  lazy val desEnv: String = config.getConfString("des.env", "local")
  lazy val desAuth: String = config.getConfString("des.authorizationToken", "Local")
  lazy val currentYearFlag: Boolean = config.getBoolean("featureFlags.currentYearFlag")
  lazy val statePensionFlag: Boolean = config.getBoolean("featureFlags.statePensionFlag")
  lazy val jobSeekersAllowanceFlag: Boolean = config.getBoolean("featureFlags.jobSeekersAllowanceFlag")

  lazy val desBaseUrl: String = config.baseUrl("des")
  lazy val citizenDetailsBaseUrl: String = config.baseUrl("citizen-details")

  def newRetryInstance(name: String, actorSystem: ActorSystem): Retry = {
    val times = config.getInt(s"microservice.services.$name.retry.times")
    val interval = getConfFiniteDuration(s"microservice.services.$name.retry.interval", 500 millis)
    new Retry(times, interval, actorSystem)
  }

  private def getConfFiniteDuration(key: String, default: FiniteDuration): FiniteDuration = {
    val d = FiniteDuration(config.getInt(key), "ms")
    require(d.isFinite(), s"not a finite duration: $key")
    FiniteDuration(d.length, d.unit)
  }

}
