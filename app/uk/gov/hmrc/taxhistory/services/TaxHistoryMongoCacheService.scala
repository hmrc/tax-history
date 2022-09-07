/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.cache.{CacheIdType, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.model.api.PayAsYouEarn
import uk.gov.hmrc.taxhistory.utils.Logging
import uk.gov.hmrc.time.TaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
  * Uses MongoDB to cache instances of `PayAsYouEarn` for a given NINO and year.
  */
class TaxHistoryMongoCacheService @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository(
      mongoComponent = mongoComponent,
      collectionName = appConfig.mongoName,
      ttl = appConfig.mongoExpiry,
      timestampSupport = timestampSupport,
      cacheIdType = CacheIdType.SimpleCacheId
    )
    with PayeCacheService
    with Logging {

  def insertOrUpdate(key: (Nino, TaxYear), value: PayAsYouEarn): Future[Option[PayAsYouEarn]] = {
    val (nino, taxYear) = key
    val mongoId         = nino.value
    val mongoKey        = taxYear.currentYear.toString
    put(mongoId)(DataKey(mongoKey), value).map(_ => Some(value)).recoverWith { case ex: Exception =>
      logger.error(s"[TaxHistoryMongoCacheService][insertOrUpdate] failed with message: ${ex.getMessage}")
      Future.successful(None)
    }
  }

  private def getFromRepository(nino: Nino, taxYear: TaxYear): Future[Option[PayAsYouEarn]] = {
    val mongoId  = nino.value
    val mongoKey = taxYear.currentYear.toString
    get[PayAsYouEarn](mongoId)(DataKey(mongoKey))
  }

  def get(key: (Nino, TaxYear)): Future[Option[PayAsYouEarn]] = {
    val (nino, taxYear) = key
    getFromRepository(nino, taxYear)
  }

  def getOrElseInsert(key: (Nino, TaxYear))(defaultToInsert: => Future[PayAsYouEarn]): Future[PayAsYouEarn] = {

    def insertDefault(): Future[PayAsYouEarn] =
      for {
        toInsert        <- defaultToInsert
        insertionResult <- insertOrUpdate(key, toInsert)
      } yield {
        if (insertionResult.isEmpty) {
          logger.warn(s"[TaxHistoryMongoCacheService][insertDefault] Cache insertion failed for $key")
        }
        toInsert
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
