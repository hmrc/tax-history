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
import play.api.libs.json.Json
import uk.gov.hmrc.cache.repository.CacheMongoRepository
import uk.gov.hmrc.mongo.{MongoSpecSupport, Saved}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

import scala.concurrent.ExecutionContext.Implicits.global


class TaxHistoryRepositoryServiceSpec extends UnitSpec
  with MockitoSugar
  with MongoSpecSupport
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with TestUtil
  {

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

    private val expireAfterInSeconds = 60

    private def repo(name: String, expiresAfter: Long = expireAfterInSeconds) = new  CacheMongoRepository(name, expiresAfter){
      await(super.ensureIndexes)
    }


  "TaxHistoryCacheService" should {

    val repository = repo("taxhistory-test")

    "successfully add the Data in cache" in {
      val cacheData = await(repository.createOrUpdate("AA000000","2015",someJson))
      cacheData.updateType shouldBe a[Saved[_]]

    }

    "fetch from the  cache by ID " in {
      val cacheData = await(repository.createOrUpdate("AA000000","2015",someJson))
      val fromCache = await(repository.findById("AA000000"))
      (fromCache.get.data.get \ "2015").get shouldBe someJson

    }


  }

  override protected def beforeEach() = {
    mongoConnectorForTest.db().drop
  }


}
