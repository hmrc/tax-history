/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mongodb.scala.model.Filters
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.model.api.{Employment, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxHistoryCacheServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with GuiceOneServerPerSuite
    with MongoSupport {

  import ITestUtil._

  val mockAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val timestampSupport = new CurrentTimestampSupport()

  val testTaxHistoryCacheService = new TaxHistoryMongoCacheService(
    mongoComponent,
    mockAppConfig,
    timestampSupport
  )

  val testPaye: PayAsYouEarn     = PayAsYouEarn()
  val testEmployment: Employment = Employment(
    startDate = Some(LocalDate.now()),
    payeReference = "SOME_PAYE",
    employerName = "Megacorp Plc",
    employmentStatus = EmploymentStatus.Live,
    worksNumber = "00191048716"
  )
  val testPaye2: PayAsYouEarn    = PayAsYouEarn(employments = List(testEmployment))

  val nino: Nino       = randomNino()
  val taxYearInt: Int  = 2015
  val taxYear: TaxYear = TaxYear(taxYearInt)

  override protected def afterAll(): Unit =
    testTaxHistoryCacheService.collection.deleteMany(Filters.empty()).toFuture().futureValue

  override def beforeEach(): Unit =
    testTaxHistoryCacheService.collection.deleteMany(Filters.empty()).toFuture().futureValue

  override def afterEach(): Unit =
    testTaxHistoryCacheService.collection.deleteMany(Filters.empty()).toFuture().futureValue

  "TaxHistoryCacheService" should {

    "successfully add the Data in cache" in {
      val cacheData = await(testTaxHistoryCacheService.insertOrUpdate((nino, taxYear), testPaye))
      cacheData shouldBe testPaye
    }

    "fetch from the cache by ID" in {
      await(for {
        _             <- testTaxHistoryCacheService.insertOrUpdate((nino, taxYear), testPaye)
        readbackValue <- testTaxHistoryCacheService.get((nino, taxYear))
      } yield readbackValue shouldBe Some(testPaye))
    }

    "When not in the mongo cache update the cache and fetch" in {
      val nino    = randomNino()
      val taxYear = TaxYear(taxYearInt - 1)

      val cacheResult0 = await(testTaxHistoryCacheService.get((nino, taxYear)))
      cacheResult0 shouldBe None

      val cacheResult1 = await(testTaxHistoryCacheService.getOrElseInsert((nino, taxYear))(Future(testPaye)))
      cacheResult1 shouldBe testPaye

      // The cache should now contain the value.
      val cacheResult2 = await(testTaxHistoryCacheService.get((nino, taxYear)))
      cacheResult2 shouldBe Some(testPaye)
    }

    "When not in the mongo cache update the cache and fetch, when request again just get" in {
      val nino    = randomNino()
      val taxYear = TaxYear(taxYearInt - 1)

      val cacheResult0 = await(testTaxHistoryCacheService.get((nino, taxYear)))
      cacheResult0 shouldBe None

      val cacheResult1 = await(testTaxHistoryCacheService.getOrElseInsert((nino, taxYear))(Future(testPaye)))
      cacheResult1 shouldBe testPaye

      // if no cache then insert testPaye2
      // but at this point I assume previous call already inserted and it will return existing data
      val cacheResult2 = await(testTaxHistoryCacheService.getOrElseInsert((nino, taxYear))(Future(testPaye2)))
      cacheResult2 shouldBe testPaye

      val cacheResult3 = await(testTaxHistoryCacheService.get((nino, taxYear)))
      cacheResult3 shouldBe Some(testPaye)
    }
  }

}
