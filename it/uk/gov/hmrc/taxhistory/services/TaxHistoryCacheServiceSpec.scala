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

package uk.gov.hmrc.taxhistory.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.model.api.PayAsYouEarn
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class TaxHistoryCacheServiceSpec extends AnyWordSpecLike with Matchers with OptionValues with ScalaFutures
  with MockitoSugar
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with GuiceOneServerPerSuite
  with MongoSpecSupport
   {

  import ITestUtil._

  val mongoComponent: ReactiveMongoComponent = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mongoConnectorForTest
  }
  val mockAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val testTaxHistoryCacheService = new TaxHistoryMongoCacheService(
    mongoComponent,
    mockAppConfig
  )

  val testPaye: PayAsYouEarn = PayAsYouEarn()

  val nino: Nino = randomNino()
  val taxYear: TaxYear = TaxYear(2015)

  override def beforeEach(): Unit = {
    mongoComponent.mongoConnector.db().drop()
  }

  "TaxHistoryCacheService" should {

    "successfully add the Data in cache" in {
      val cacheData = await(testTaxHistoryCacheService.insertOrUpdate((nino, taxYear), testPaye))
      cacheData shouldBe Some(testPaye)
    }

    "fetch from the cache by ID" in {
      await(for {
        _ <- testTaxHistoryCacheService.insertOrUpdate((nino, taxYear), testPaye)
        readbackValue <- testTaxHistoryCacheService.get((nino, taxYear))
      } yield {
        readbackValue shouldBe Some(testPaye)
      })
    }

    "When not in the mongo cache update the cache and fetch" in {
      val nino = randomNino()
      val taxYear = TaxYear(2014)

      val cacheResult0 = await(testTaxHistoryCacheService.get((nino, taxYear)))
      cacheResult0 shouldBe None
      val cacheResult1 = await(testTaxHistoryCacheService.getOrElseInsert((nino, taxYear))(Future(testPaye)))
      cacheResult1 shouldBe testPaye
      // The cache should now contain the value.
      val cacheResult2 = await(testTaxHistoryCacheService.get((nino, taxYear)))
      cacheResult2 shouldBe Some(testPaye)
    }
  }

  override protected def afterAll(): Unit = {
    mongoComponent.mongoConnector.db().drop()
  }
}
