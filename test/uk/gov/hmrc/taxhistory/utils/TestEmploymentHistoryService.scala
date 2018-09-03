/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.connectors.{DesNpsConnector, RtiConnector, SquidNpsConnector}
import uk.gov.hmrc.taxhistory.services.EmploymentHistoryService

object TestEmploymentHistoryService extends AnyRef with MockitoSugar {
  def createNew(): EmploymentHistoryService =
    new EmploymentHistoryService(
      desNpsConnector = mock[DesNpsConnector],
      squidNpsConnector = mock[SquidNpsConnector],
      rtiConnector = mock[RtiConnector],
      cacheService = TestPayeCacheService(),
      auditable = mock[Auditable],
      currentYearFlag = true,
      statePensionFlag = true,
      jobSeekersAllowanceFlag = true
    )
}