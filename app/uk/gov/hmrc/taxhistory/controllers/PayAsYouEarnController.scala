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

package uk.gov.hmrc.taxhistory.controllers

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.taxhistory.services.{EmploymentHistoryService, SaAuthService}
import uk.gov.hmrc.time.TaxYear

class PayAsYouEarnController @Inject()(employmentHistoryService: EmploymentHistoryService,
                                       saAuthService: SaAuthService) extends TaxHistoryController {

  def getPayAsYouEarn(nino: Nino, taxYear: TaxYear) = {
    saAuthService.checkSaAuthorisation(nino).async { implicit request =>
      toResult(employmentHistoryService.getFromCache(nino, taxYear))
    }
  }
}