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

import play.api.libs.json.JsValue
import play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.cache.model.{Cache, Id}
import uk.gov.hmrc.cache.repository.CacheMongoRepository
import uk.gov.hmrc.taxhistory.config.ApplicationConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TaxHistoryCacheService extends MongoDbConnection{
  def cacheRepository: CacheMongoRepository
  def mongoSource:String

   def createOrUpdate(id: String, key: String, toCache: JsValue): Future[Option[JsValue]] = {
    cacheRepository.createOrUpdate(Id(id),key,toCache).map(x => x.updateType.savedValue.data)
  }

   def findById(id: String): Future[Option[JsValue]] = {
    cacheRepository.findById(Id(id)).map(_.get.data)
  }




}

object TaxHistoryCacheService extends TaxHistoryCacheService {
  override def cacheRepository : CacheMongoRepository = new CacheMongoRepository(
    mongoSource,expireAfterSeconds = ApplicationConfig.expireAfterSeconds,Cache.mongoFormats)

  override def mongoSource = ApplicationConfig.mongoSource


}
