/*
 * Copyright 2024 HM Revenue & Customs
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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.after
import play.api.Logging
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class Retry @Inject() (val times: Int, val delay: FiniteDuration, val system: ActorSystem) extends Logging {

  private def apply[A](n: Int)(f: => Future[A])(implicit ec: ExecutionContext): Future[A] =
    f.recoverWith {
      case ShouldRetryAfter(e) if n > 0 =>
        logger.warn(s"[Retry][apply] Retrying after failure ${e.getMessage}")
        after(delay, system.scheduler)(apply(n - 1)(f))
    }

  def apply[A](f: => Future[A])(implicit ec: ExecutionContext): Future[A] =
    apply(times)(f)

  private object ShouldRetryAfter {
    def unapply(e: Exception): Option[Exception] = e match {
      case ex: GatewayTimeoutException            => Some(ex)
      case ex: BadGatewayException                => Some(ex)
      case ex @ UpstreamErrorResponse(_, _, _, _) => Some(ex)
      case _                                      => None
    }
  }
}
