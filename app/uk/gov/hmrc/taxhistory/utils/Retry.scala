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

package uk.gov.hmrc.taxhistory.utils

import akka.actor.ActorSystem
import akka.pattern.after
import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, UpstreamErrorResponse}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class Retry @Inject()(val times: Int, val delay: FiniteDuration, val system: ActorSystem) {

  private def apply[A](n: Int = times)(f: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    f.recoverWith {
      case ShouldRetryAfter(e) if n > 0 =>
        Logger.warn(s"Retrying after failure $e")
        after(delay, system.scheduler)(apply(n - 1)(f))
    }
  }

  def apply[A](f: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    apply(times)(f)
  }


  private object ShouldRetryAfter {
    def unapply(e: Exception): Option[Exception] = e match {
      case ex: GatewayTimeoutException => Some(ex)
      case ex: BadGatewayException => Some(ex)
      case ex @ UpstreamErrorResponse(_, _, _, _) => Some(ex)
      case _ => None
    }
  }
}
