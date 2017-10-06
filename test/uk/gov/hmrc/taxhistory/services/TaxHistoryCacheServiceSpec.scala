/*
 * Copyright 2017 HM Revenue & Customs
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


import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Sequential}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.cache.repository.CacheMongoRepository
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.config.ApplicationConfig
import uk.gov.hmrc.taxhistory.model.utils.TestUtil
import uk.gov.hmrc.taxhistory.services.TaxHistoryCacheService.mongoSource

import scala.concurrent.ExecutionContext.Implicits.global



class TaxHistoryCacheServiceSpec extends UnitSpec
  with MockitoSugar
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with TestUtil
  with GuiceOneServerPerSuite{
  val expireAfterSeconds: Int = 10

  object TestTaxHistoryCacheService extends TaxHistoryCacheService{
    override def cacheRepository: CacheMongoRepository =  new CacheMongoRepository(
      mongoSource,expireAfterSeconds = expireAfterSeconds,Cache.mongoFormats)

    override def mongoSource: String = "tax-history-test"
  }

  val someJson  =  Json.parse(""" [{
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

  "TaxHistoryCacheService" should {

      "successfully add the Data in cache" in {
         val cacheData = await(TestTaxHistoryCacheService.createOrUpdate("AA000000","2015",someJson))
          (cacheData.get \ "2015").get shouldBe someJson
      }

      "fetch from the  cache by ID " in {
         val fromCache = await(TestTaxHistoryCacheService.findById("AA000000"))
         (fromCache.get \ "2015").get shouldBe someJson
      }


  }

  override protected def afterAll() = {
   TaxHistoryCacheService.mongoConnector.db().drop()
  }


}
