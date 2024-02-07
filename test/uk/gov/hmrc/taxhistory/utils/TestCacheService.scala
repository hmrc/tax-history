/*
 * Copyright 2004 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.utils

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.taxhistory.model.api.PayAsYouEarn
import uk.gov.hmrc.taxhistory.services.{CacheService, PayeCacheService}
import uk.gov.hmrc.time.TaxYear
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * A simple in-memory implementation of the cache service.
  * Values never expire.
  */
class TestCacheService[K, V]() extends CacheService[K, V] {

  var map: Map[K, V] = Map.empty

  def insertOrUpdate(key: K, value: V): Future[V] = Future.successful {
    map = map + (key -> value)
    map.getOrElse(key, throw new RuntimeException("Failed to insert into cache"))
  }

  def get(key: K): Future[Option[V]] = Future.successful(map.get(key))

  def getOrElseInsert(key: K)(defaultToInsert: => Future[V]): Future[V] =
    map.get(key) match {
      case Some(value) => Future.successful(value)
      case None        =>
        defaultToInsert.map { default =>
          map = map + (key -> default)
          default
        }
    }
}

case class TestPayeCacheService() extends TestCacheService[(Nino, TaxYear), PayAsYouEarn] with PayeCacheService
