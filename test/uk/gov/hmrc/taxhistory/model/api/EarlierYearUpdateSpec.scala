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

package uk.gov.hmrc.taxhistory.model.api

import java.time.LocalDate
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.taxhistory.utils.{DateUtils, TestUtil}

import java.util.UUID

class EarlierYearUpdateSpec extends TestUtil with AnyWordSpecLike with Matchers with OptionValues with DateUtils {

  lazy val earlierYearUpdateJson: JsValue     = loadFile("/json/model/api/earlierYearUpdate.json")
  lazy val earlierYearUpdateListJson: JsValue = loadFile("/json/model/api/earlierYearUpdates.json")

  lazy val earlierYearUpdate1: EarlierYearUpdate = EarlierYearUpdate(
    earlierYearUpdateId = UUID.fromString("cf1886e7-ae56-4ec2-84a6-926d64ace287"),
    taxablePayEYU = BigDecimal(6543.21),
    taxEYU = BigDecimal(123.45),
    receivedDate = LocalDate.of(YEAR_2016, JUNE, DAY_26)
  )

  lazy val earlierYearUpdate2: EarlierYearUpdate          = EarlierYearUpdate(
    earlierYearUpdateId = UUID.fromString("effa7845-aa97-454f-88da-ffa099eba7f2"),
    taxablePayEYU = BigDecimal(123.45),
    taxEYU = BigDecimal(67.89),
    receivedDate = LocalDate.of(YEAR_2015, MAY, DAY_29)
  )
  lazy val earlierYearUpdateList: List[EarlierYearUpdate] = List(earlierYearUpdate1, earlierYearUpdate2)

  "EarlierYearUpdate" should {

    "transform into Json from object correctly " in {
      Json.toJson(earlierYearUpdate1) shouldBe earlierYearUpdateJson
    }
    "transform into object from json correctly " in {
      earlierYearUpdateJson.as[EarlierYearUpdate] shouldBe earlierYearUpdate1
    }
    "generate employmentId when none is supplied" in {
      val eyu = EarlierYearUpdate(
        taxablePayEYU = BigDecimal(1.11),
        taxEYU = BigDecimal(22.22),
        receivedDate = LocalDate.of(YEAR_2015, MAY, DAY_29)
      )

      eyu.earlierYearUpdateId.toString.nonEmpty shouldBe true
      eyu.earlierYearUpdateId                  shouldNot be(earlierYearUpdate1.earlierYearUpdateId)
    }
    "transform into Json from object list correctly " in {
      Json.toJson(earlierYearUpdateList) shouldBe earlierYearUpdateListJson
    }
    "transform into object list from json correctly " in {
      earlierYearUpdateListJson.as[List[EarlierYearUpdate]] shouldBe earlierYearUpdateList
    }
  }
}
