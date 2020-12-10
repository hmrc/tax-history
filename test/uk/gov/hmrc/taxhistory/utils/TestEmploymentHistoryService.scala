/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.connectors.{DesNpsConnector, RtiConnector}
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService

import scala.concurrent.ExecutionContext.Implicits.global

object TestEmploymentHistoryService extends MockitoSugar  {

  val mockAppConfig: AppConfig = mock[AppConfig]
  when(mockAppConfig.currentYearFlag).thenReturn(true)

  def createNew(): EmploymentHistoryService =
    new EmploymentHistoryService(
      desNpsConnector = mock[DesNpsConnector],
      rtiConnector = mock[RtiConnector],
      cacheService = TestPayeCacheService(),
      auditable = mock[Auditable],
      config = mockAppConfig
    )
}