/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.model.rti

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.model.rti.RtiData

import scala.io.Source

/**
  * Created by kris on 13/06/17.
  */
class RtiDataSpec extends TestUtil with UnitSpec {

  lazy val rtiSuccessfulResponseURLDummy = loadFile("/json/rti/response/dummyRti.json")

  "RtiData" should {
    "transform Rti Response Json correctly to RtiData Model " in {
      val rtiDetails = rtiSuccessfulResponseURLDummy.as[RtiData](RtiData.reader)
      rtiDetails shouldBe a[RtiData]
      rtiDetails.nino shouldBe ""

    }
    "transform Rti Response Json correctly containing Employments" in {
      val rtiDetails = rtiSuccessfulResponseURLDummy.as[RtiData](RtiData.reader)
      rtiDetails shouldBe a[RtiData]

    }
    "transform Rti Response Json correctly which containing Payments" in {
      val rtiDetails = rtiSuccessfulResponseURLDummy.as[RtiData](RtiData.reader)
      rtiDetails shouldBe a[RtiData]

    }
    "transform Rti Response Json correctly which containing EndOfYearUpdates" in {
      val rtiDetails = rtiSuccessfulResponseURLDummy.as[RtiData](RtiData.reader)
      rtiDetails shouldBe a[RtiData]

    }
  }
}

object TestUtil extends TestUtil

trait TestUtil {
  def loadFile(path:String): JsValue = {
    val jsonString = Source.fromURL(getClass.getResource(path)).mkString
    Json.parse(jsonString)
  }
}
