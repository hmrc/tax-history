/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.controllers.test

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.ControllerComponents
import play.api.test.Helpers._
import uk.gov.hmrc.taxhistory.services.TaxHistoryMongoCacheService
import uk.gov.hmrc.taxhistory.utils.TestUtil
import play.api.test.FakeRequest
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.taxhistory.config.AppConfig

import scala.concurrent.ExecutionContext

class TestOnlyControllerSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with TestUtil
    with BeforeAndAfterEach
    with MongoSupport {
  val mockAppConfig: AppConfig                    = app.injector.instanceOf[AppConfig]
  val timestampSupport                            = new CurrentTimestampSupport()
  val cc: ControllerComponents                    = stubControllerComponents()
  implicit val executionContext: ExecutionContext = cc.executionContext
  val testTaxHistoryCacheService                  = new TaxHistoryMongoCacheService(
    mongoComponent,
    mockAppConfig,
    timestampSupport
  )
  val testOnlyController                          = new TestOnlyController(testTaxHistoryCacheService, cc)
  "clearCache" should {
    "clear the cache" in {
      val result = testOnlyController.clearCache.apply(FakeRequest())
      status(result) must be(OK)
    }
  }
}
