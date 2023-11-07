/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.services

import org.mongodb.scala.model.Filters
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.model.api.{Allowance, PayAsYouEarn}
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global

class TaxHistoryRepositoryServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ScalaFutures
    with MongoSupport
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with GuiceOneServerPerSuite {

  val mockAppConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val mockTimeStampSupport     = new CurrentTimestampSupport()
  val repository               = new TaxHistoryMongoCacheService(mongoComponent, mockAppConfig, mockTimeStampSupport)

  override def beforeEach(): Unit =
    repository.collection.deleteMany(Filters.empty()).toFuture().futureValue

  override def afterEach(): Unit =
    repository.collection.deleteMany(Filters.empty()).toFuture().futureValue

  "TaxHistoryCacheService" should {

    val payAsYouEarn = PayAsYouEarn(allowances = List(Allowance(iabdType = "a", amount = 100.0)))

    "successfully add the Data in cache" in {
      val result = await(repository.insertOrUpdate((Nino("AA000000A"), TaxYear(2015)), payAsYouEarn))
      result shouldBe payAsYouEarn
    }

    "fetch from the cache by ID " in {
      await(repository.insertOrUpdate((Nino("AA000000A"), TaxYear(2015)), payAsYouEarn))
      val readbackData = await(repository.findById("AA000000A"))
      readbackData                                          shouldBe defined
      (readbackData.get.data \ "2015").get.as[PayAsYouEarn] shouldBe payAsYouEarn
    }
  }
}
