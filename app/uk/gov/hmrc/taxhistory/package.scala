/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc

import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.{ExecutionContext, Future}

package object taxhistory {

  implicit class FutureListOps[A](fl: Future[List[A]])(implicit ec: ExecutionContext) {
    /**
      * Converts a future containing an empty collection into a failure with NotFoundException
      */
    def orNotFound(message: String): Future[List[A]] = fl.flatMap {
      case Nil => Future.failed(new NotFoundException(message))
      case list@_ => Future.successful(list)
    }
  }

  implicit class FutureMapOps[A, B](fl: Future[Map[A, B]])(implicit ec: ExecutionContext) {
    /**
      * Converts a future containing an empty map into a failure with NotFoundException
      */
    def orNotFound(message: String): Future[Map[A, B]] = fl.flatMap {
      case map@_ if map.isEmpty => Future.failed(new NotFoundException(message))
      case map@_ => Future.successful(map)
    }
  }

  implicit class FutureOptionOps[A](fl: Future[Option[A]])(implicit ec: ExecutionContext) {
    /**
      * Converts a future containing an empty option into a failure with NotFoundException
      */
    def orNotFound(message: String): Future[Option[A]] = fl.flatMap {
      case None => Future.failed(new NotFoundException(message))
      case Some(a) => Future.successful(Some(a))
    }
  }

}
