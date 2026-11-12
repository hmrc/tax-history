/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.connectors

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.utils.TestUtil

class HeaderHelperSpec extends PlaySpec
  with TestUtil
  with BeforeAndAfterEach
  with GuiceOneAppPerSuite {

  val config = app.injector.instanceOf[AppConfig]
  val headersUtil = new HeaderHelper(config)
  implicit val hc: HeaderCarrier = new HeaderCarrier()

  "create the correct headers" in {
    val expectedDesHeaders = List(
      ("Environment", "local"),
      ("Authorization", "Bearer Local"),
      ("CorrelationId", "123f4567-g89c-42c3-b456-557742330000")
    )
    val expectedHipHeaders = List(
      ("Authorization","Basic YXBpLWNsaWVudC1pZDphcGktY2xpZW50LXNlY3JldA=="),
      ("correlationid","85c4bf78-71da-4f16-b494-447aaba2ca3c"),
      ("X-Receipt-Date","2026-11-12T14:35:14Z"),
      ("X-Transmitting-System","HIP"),
      ("gov-uk-originator-id","MDTP-PAYE-TES-2")
    )
    val headers = headersUtil.createHeaders
    if (config.api5990) headers mustBe expectedHipHeaders else
      headers mustBe expectedDesHeaders
  }
}
