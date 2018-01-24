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

import javax.inject.{Inject, Named}

import play.api.libs.json.JsValue
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import uk.gov.hmrc.cache.model.{Cache, Id}
import uk.gov.hmrc.cache.repository.CacheMongoRepository
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxHistoryCacheService @Inject()(
                              val mongoDbConnection: MongoDbConnection,
                              @Named("mongodb.cache.expire.seconds") expireAfterSeconds: Int,
                              @Named("mongodb.name") mongoSource: String) {

  implicit val mongo: () => DB = mongoDbConnection.db

  val cacheRepository = new CacheMongoRepository(mongoSource, expireAfterSeconds, Cache.mongoFormats)

   def createOrUpdate(id: String, key: String, toCache: JsValue): Future[Option[JsValue]] = {
    cacheRepository.createOrUpdate(Id(id), key, toCache).map(x => x.updateType.savedValue.data.map(_ \ key).map(_.get))
  }

   def findById(id: String, taxYear: Int): Future[Option[JsValue]] = {
    cacheRepository.findById(Id(id)).map(_.flatMap(_.data).map(_ \ taxYear.toString).flatMap(_.toOption))
  }

  def get(nino: Nino, year: TaxYear): Future[Option[JsValue]] = findById(nino.nino, year.currentYear)

  def getOrElseInsert(nino: Nino, year: TaxYear)(defaultToInsert : => Future[JsValue]): Future[Option[JsValue]] = {
    get(nino, year).flatMap {
      case Some(x) => Future.successful(Some(x))
      case _       => defaultToInsert.flatMap(js => createOrUpdate(nino.nino, year.currentYear.toString, js))
    }
  }
}