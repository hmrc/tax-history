/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class RetrySpec extends UnitSpec {


  private val system = ActorSystem("test")
  private val delay = FiniteDuration(1000, TimeUnit.MILLISECONDS)
  private val retry = new Retry(2, delay, system)

  "RetrySpec" should {
    "return result when the operation succeeds" in {
      def op = Future {
        100
      }

      val result = await(retry(op))

      result shouldBe 100
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
      var tries = 0

      val result = await(retry {
        Future {
          tries = tries + 1
          if (tries <= 2)
            throw new BadGatewayException("Some error happened. Please try again later.")
          else
            100
        }
      })

      result shouldBe 100
      tries shouldBe 3
    }

    "return exception after retrying 3 times the given operation always returns exception" in {
      def op = Future {
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