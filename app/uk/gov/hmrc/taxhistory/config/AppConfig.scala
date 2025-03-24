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

package uk.gov.hmrc.taxhistory.config

import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.taxhistory.utils.Retry

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration}
import java.util.Base64

@Singleton
class AppConfig @Inject() (config: ServicesConfig) {

  lazy val appName: String       = config.getString("appName")
  lazy val mongoExpiry: Duration = Duration(config.getInt("mongodb.cache.expire.seconds"), TimeUnit.SECONDS)
  lazy val mongoName: String     = config.getString("mongodb.name")
  lazy val desEnv: String        = config.getConfString("des.env", "local")
  lazy val desAuth: String       = config.getConfString("des.authorizationToken", "Local")

  lazy val npsDesBaseUrl: String         = config.baseUrl("nps.des")
  lazy val rtiDesBaseUrl: String         = config.baseUrl("rti.des")
  lazy val citizenDetailsBaseUrl: String = config.baseUrl("citizen-details")
  lazy val isUsingHIP: Boolean           = config.getBoolean("feature.isUsingHIP")
  lazy val hipBaseUrl: String            = config.baseUrl("nps.hip")
  private val clientIdV1: String         = config.getString("microservice.services.nps.hip.clientId")
  private val secretV1: String           = config.getString("microservice.services.nps.hip.secret")
  def authorizationToken: String         = Base64.getEncoder.encodeToString(s"$clientIdV1:$secretV1".getBytes("UTF-8"))
  val hipServiceOriginatorIdKey: String  = config.getString("microservice.services.nps.hip.originatoridkey")
  val hipServiceOriginatorId: String     = config.getString("microservice.services.nps.hip.originatoridvalue")

  def newRetryInstance(name: String, actorSystem: ActorSystem): Retry = {
    val times    = config.getInt(s"microservice.services.$name.retry.times")
    val interval = getConfFiniteDuration(s"microservice.services.$name.retry.interval")
    new Retry(times, interval, actorSystem)
  }

  private def getConfFiniteDuration(key: String): FiniteDuration = {
    val d = FiniteDuration(config.getInt(key), "ms")
    require(d.isFinite, s"not a finite duration: $key")
    FiniteDuration(d.length, d.unit)
  }

}
