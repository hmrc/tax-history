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

package uk.gov.hmrc

import _root_.play.api.libs.json.JsResultException
import _root_.play.api.libs.json.Reads
import _root_.play.api.http.Status
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

package object taxhistory {

  implicit class HttpResponseFutureOps(responseFuture: Future[HttpResponse])(implicit ec: ExecutionContext) {
//    def decodeJsonOrError[A : Reads]: Future[A] = for {
//      response   <- responseFuture
//      okResponse <- response.expectOk
//      decoded    <- okResponse.decodeJsonOrError[A]
//    } yield {
//      decoded
//    }
  }

  implicit class HttpResponseOps(response: HttpResponse)(implicit ec: ExecutionContext) {
    def orNotFound(itemType: Class[_], id: Any): Future[HttpResponse] = response.status match {
      case Status.OK        => Future.successful(response)
      case Status.NOT_FOUND => Future.failed(TaxHistoryException.notFound(itemType, id))
      case status           => Future.failed(TaxHistoryException(HttpNotOk(response.status, response)))
    }

    def expectOk: Future[HttpResponse] = {
      if (response.status == Status.OK) {
        Future.successful(response)
      } else {
        Future.failed(TaxHistoryException(HttpNotOk(response.status, response)))
      }
    }

    def decodeJsonOrError[A : Reads]: Future[A] = {
      if (response.status == Status.OK) {
        Try(response.json.as[A]) match {
          case Success(decoded)                        => Future.successful[A](decoded)
          case Failure(jsException: JsResultException) => Future.failed[A](TaxHistoryException(JsonParsingError(jsException)))
          case Failure(throwable)                      => Future.failed[A](TaxHistoryException(UnknownError(throwable)))
        }
      } else {
        Future.failed[A](TaxHistoryException(HttpNotOk(response.status, response)))
      }
    }
  }

}
