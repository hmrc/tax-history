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

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.{DB, DefaultDB}
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global


class TaxHistoryCacheServiceSpec extends UnitSpec
  with MockitoSugar
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with TestUtil
  with GuiceOneServerPerSuite
  with MongoSpecSupport {

  val testMongoDbConnection = new MongoDbConnection {
    override implicit val db: () => DefaultDB = mongo // this value comes from the trait MongoSpecSupport
    override lazy val mongoConnector: MongoConnector = mongoConnectorForTest // this value comes from the trait MongoSpecSupport
  }

  val testTaxHistoryCacheService = new TaxHistoryCacheService(
    mongoDbConnection = testMongoDbConnection,
    expireAfterSeconds = 10,
    mongoSource = "tax-history-test"
  )

  val someJson = Json.parse(""" [{
                             |    "nino": "AA000000",
                             |    "sequenceNumber": 1,
                             |    "worksNumber": "6044041000000",
                             |    "taxDistrictNumber": "531",
                             |    "payeNumber": "J4816",
                             |    "employerName": "Aldi",
                             |    "receivingJobseekersAllowance" : false,
                             |    "otherIncomeSourceIndicator" : false,
                             |    "startDate": "21/01/2015"
                             |    }]
                           """.stripMargin)

  val nino = randomNino()

  override def beforeEach() = {
    testMongoDbConnection.mongoConnector.db().drop()
  }

  "TaxHistoryCacheService" should {

    "successfully add the Data in cache" in {
       val cacheData = await(testTaxHistoryCacheService.createOrUpdate(nino.nino,"2015",someJson))
        cacheData shouldBe Some(someJson)
    }

    "fetch from the cache by ID" in {
      await(for {
        _ <- testTaxHistoryCacheService.createOrUpdate(nino.nino,"2015",someJson)
        readbackValue <- testTaxHistoryCacheService.findById(nino.nino, 2015)
      } yield {
        readbackValue shouldBe Some(someJson)
      })
    }

    "When not in the mongo cache update the cache and fetch" in {
      val nino = randomNino()
      val taxYear = TaxYear(2014)

      val cacheResult0 = await(testTaxHistoryCacheService.get(nino, taxYear))
      cacheResult0 shouldBe None
      val cacheResult1 = await(testTaxHistoryCacheService.getOrElseInsert(nino, taxYear)(someJson))
      cacheResult1 shouldBe Some(someJson)
      // The cache should now contain the value.
      val cacheResult2 = await(testTaxHistoryCacheService.get(nino, taxYear))
      cacheResult2 shouldBe Some(someJson)
    }
  }

  override protected def afterAll() = {
    testMongoDbConnection.mongoConnector.db().drop()
  }
}
