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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.model.api.{Employment, PayAsYouEarn}
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

class TaxHistoryCacheServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with BeforeAndAfterEach
    with GuiceOneServerPerSuite
    with MongoSupport {

  implicit val patientConfig: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 1.seconds)

  def randomNino(): Nino = Nino(new Generator(new Random()).nextNino.value.replaceFirst("MA", "AA"))

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

  val currentTaxYearAsInt: Int  = 2015
  val previousTaxYearAsInt: Int = currentTaxYearAsInt - 1

  override def beforeEach(): Unit =
    testTaxHistoryCacheService.collection.deleteMany(Filters.empty()).toFuture().futureValue

  override def afterEach(): Unit =
    testTaxHistoryCacheService.collection.deleteMany(Filters.empty()).toFuture().futureValue

  "TaxHistoryCacheService" should {

    "successfully add the Data in cache" in {
      val nino    = randomNino()
      val taxYear = TaxYear(currentTaxYearAsInt)

      val insertedData = testTaxHistoryCacheService.insertOrUpdate((nino, taxYear), testPaye).futureValue
      insertedData shouldBe testPaye
    }

    "fetch from the cache by ID" in {
      val nino    = randomNino()
      val taxYear = TaxYear(currentTaxYearAsInt)

      val insertedData = testTaxHistoryCacheService.insertOrUpdate((nino, taxYear), testPaye).futureValue
      insertedData shouldBe testPaye

      val paye = testTaxHistoryCacheService.get((nino, taxYear)).futureValue
      paye shouldBe Some(testPaye)
    }

    "When not in the mongo cache update the cache and fetch" in {
      val nino    = randomNino()
      val taxYear = TaxYear(previousTaxYearAsInt)

      val cacheResult0 = testTaxHistoryCacheService.get((nino, taxYear)).futureValue
      cacheResult0 shouldBe None

      val cacheResult1 = testTaxHistoryCacheService.getOrElseInsert((nino, taxYear))(Future(testPaye)).futureValue
      cacheResult1 shouldBe testPaye

      // The cache should now contain the value.
      val cacheResult2 = testTaxHistoryCacheService.get((nino, taxYear)).futureValue
      cacheResult2 shouldBe Some(testPaye)
    }

    "When not in the mongo cache update the cache and fetch, when request again just get it" in {
      val nino    = randomNino()
      val taxYear = TaxYear(previousTaxYearAsInt)

      val cacheResult0 = testTaxHistoryCacheService.get((nino, taxYear)).futureValue
      cacheResult0 shouldBe None

      val cacheResult1 = testTaxHistoryCacheService.getOrElseInsert((nino, taxYear))(Future(testPaye)).futureValue
      cacheResult1 shouldBe testPaye

      // if no cache then insert testPaye2
      // but at this point I assume previous call already inserted and it will return existing data
      val cacheResult2 = testTaxHistoryCacheService.getOrElseInsert((nino, taxYear))(Future(testPaye2)).futureValue
      cacheResult2 shouldBe testPaye

      val cacheResult3 = testTaxHistoryCacheService.get((nino, taxYear)).futureValue
      cacheResult3 shouldBe Some(testPaye)
    }

    "When not in the mongo cache try to update the cache and fail" in {
      val nino    = randomNino()
      val taxYear = TaxYear(previousTaxYearAsInt)

      val cacheResult = testTaxHistoryCacheService.get((nino, taxYear)).futureValue
      cacheResult shouldBe None

      // if no cache and result to fill the cache failed then return exception as it is
      intercept[RuntimeException] {
        testTaxHistoryCacheService
          .getOrElseInsert((nino, taxYear))(Future.failed(new RuntimeException("test exception")))
          .futureValue
      }

      val cacheResult1 = testTaxHistoryCacheService.get((nino, taxYear)).futureValue
      cacheResult1 shouldBe None
    }
  }

}
