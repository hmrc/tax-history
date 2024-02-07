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

import org.apache.pekko.actor.ActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException}

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class RetrySpec extends AnyWordSpec with Matchers with OptionValues with ScalaFutures {

  private val lengthOfTime = 1000
  private val system       = ActorSystem("test")
  private val delay        = FiniteDuration(lengthOfTime, TimeUnit.MILLISECONDS)
  private val retry        = new Retry(2, delay, system)

  "RetrySpec" should {
    "return result when the operation succeeds" in {
      val futureInt       = 100
      def op: Future[Int] = Future {
        futureInt
      }

      val result = retry(op).futureValue

      result shouldBe futureInt
    }

    "retry 3 times the given the operation fails all the time" in {
      var tries = 0

      an[GatewayTimeoutException] should be thrownBy {
        await(retry {
          Future {
            tries = tries + 1
            throw new GatewayTimeoutException("Some error happened. Please try again later.")
          }
        })
      }

      tries shouldBe 3
    }

    "succeed on third trial after retrying twice" in {
      var tries      = 0
      val oneHundred = 100

      val result = await(retry {
        Future {
          tries = tries + 1
          if (tries <= 2) {
            throw new BadGatewayException("Some error happened. Please try again later.")
          } else {
            oneHundred
          }
        }
      })

      result shouldBe 100
      tries  shouldBe 3
    }

    "return exception after retrying 3 times the given operation always returns exception" in {
      def op: Future[Nothing] = Future {
        throw new BadGatewayException("Some error happened. Please try again later.")
      }

      an[BadGatewayException] should be thrownBy {
        await(retry(op))
      }
    }

    "not retry 3 times the given the operation fails with a different exception (eg: IllegalStateException)" in {
      var tries = 0

      an[IllegalStateException] should be thrownBy {
        await(retry {
          Future {
            tries = tries + 1
            throw new IllegalStateException("Some error happened. Please try again later.")
          }
        })
      }

      tries shouldBe 1
    }

  }
}
