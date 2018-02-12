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

import play.api.libs.json.Json
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import uk.gov.hmrc.cache.model.{Cache, Id}
import uk.gov.hmrc.cache.repository.CacheMongoRepository
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.taxhistory.model.api.PayAsYouEarn
import uk.gov.hmrc.taxhistory.utils.Logging
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.{ExecutionContext, Future}

/**
  * Uses MongoDB to cache instances of `PayAsYouEarn` for a given NINO and year.
  */
class TaxHistoryMongoCacheService @Inject()(
                              val mongoDbConnection: MongoDbConnection,
                              @Named("mongodb.cache.expire.seconds") expireAfterSeconds: Int,
                              @Named("mongodb.name") mongoSource: String)(implicit ec: ExecutionContext) extends PayeCacheService with Logging {

  implicit val mongo: () => DB = mongoDbConnection.db

  val cacheRepository = new CacheMongoRepository(mongoSource, expireAfterSeconds, Cache.mongoFormats)

  def insertOrUpdate(key: (Nino, TaxYear), value: PayAsYouEarn): Future[Option[PayAsYouEarn]] = {
    val (nino, taxYear) = key
    val mongoId = nino.value
    val mongoKey = taxYear.currentYear.toString
    cacheRepository
      .createOrUpdate(Id(mongoId), mongoKey, Json.toJson[PayAsYouEarn](value))
      .map(_.updateType.savedValue.data.map(_ \ mongoKey).map(_.get.as[PayAsYouEarn]))
  }

  private def getFromRepository(nino: Nino, taxYear: TaxYear): Future[Option[PayAsYouEarn]] = {
    val mongoId = nino.value
    val mongoKey = taxYear.currentYear.toString
    cacheRepository
      .findById(Id(mongoId))
      .map(_.flatMap(_.data).map(_ \ mongoKey).flatMap(_.toOption).map(_.as[PayAsYouEarn]))
  }

  def get(key: (Nino, TaxYear)): Future[Option[PayAsYouEarn]] = {
    val (nino, taxYear) = key
    getFromRepository(nino, taxYear)
  }

  def getOrElseInsert(key: (Nino, TaxYear))(defaultToInsert : => Future[PayAsYouEarn]): Future[PayAsYouEarn] = {

    def insertDefault(): Future[PayAsYouEarn] = {
      for {
        toInsert <- defaultToInsert
        insertionResult <- insertOrUpdate(key, toInsert)
      } yield {
        if (insertionResult.isEmpty) logger.warn(s"Cache insertion failed for $key")
        toInsert
      }
    }

    for {
      cacheResult <- get(key)
      returnValue <- cacheResult match {
                        case Some(hit) => Future.successful(hit)
                        case None      => insertDefault()
                      }
    } yield returnValue
  }
}