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

  /*
  Please note that the following conversions from HttpResponse to Successful/failed futures are only appropriate
  as long as we are only performing GET requests. If we perform other types of requests we need to take into account
  other possible response codes which are not the simple 'OK' but are still successful.
   */

  /**
    * An enriched utility method for `HttpResponse`.
    * If the response is 'not found', tag the resulting failed Future with the details of the item that was not found.
    * If the response is successful, produce a successful Future.
    * If any other failure occurred, produce a failed future as appropriate.
    */
  implicit class HttpResponseOps(response: HttpResponse)(implicit ec: ExecutionContext) {
    def orNotFound(itemType: Class[_], id: Any): Future[HttpResponse] = response.status match {
      case Status.NOT_FOUND             => Future.failed(TaxHistoryException.notFound(itemType, id))
      case other @ _                    => response.expectOk
    }

    /**
      * Convert a `HttpResponse` into a successful or failed future.
      * This enables an alternative modelling of HTTP failures which helps to combine it
      * with other Futures and other failures.
      */
    def expectOk: Future[HttpResponse] = response.status match {
      case Status.OK                    => Future.successful(response)
      case Status.NOT_FOUND             => Future.failed(TaxHistoryException.notFound(classOf[Any], ""))
      case Status.SERVICE_UNAVAILABLE   => Future.failed(TaxHistoryException.serviceUnavailable)
      case Status.INTERNAL_SERVER_ERROR => Future.failed(TaxHistoryException.internalServerError)
      case Status.BAD_REQUEST           => Future.failed(TaxHistoryException.badRequest)
      case otherStatus                  => Future.failed(TaxHistoryException(GenericHttpError(otherStatus, response)))
    }

    /**
      * An enriched utility method for `HttpResponse`.
      * If the response is successful, attempt to parse and instantiate it as the given type.
      * If the response is 'not found', tag the resulting failed future with the details of the item that was not found.
      * If any other failure occurred, produce a failed future as appropriate.
      */
    def decodeJsonOrNotFound[A : Reads](itemType: Class[_], id: Any): Future[A] = {
      response.orNotFound(itemType, id).flatMap { resp =>
        Try(resp.json.as[A]) match {
          case Success(decoded)                        => Future.successful[A](decoded)
          case Failure(jsException: JsResultException) => Future.failed[A](TaxHistoryException(JsonParsingError(jsException)))
          case Failure(throwable)                      => Future.failed[A](TaxHistoryException(UnknownError(throwable)))
        }
      }
    }
  }

  implicit class TaxHistoryExceptionFailedFutureOps[A](fa: Future[A])(implicit ec: ExecutionContext) {
    /**
      * If this Future has failed with a `TaxHistoryException`, set the 'originator' field to the given value,
      * to allow tracing of the service where this failure took place.
      */
    def tagWithOriginator(originator: String): Future[A] = {
      fa.recoverWith {
        case TaxHistoryException(error, _) => Future.failed(TaxHistoryException(error, Some(originator)))
      }
    }
  }
}
