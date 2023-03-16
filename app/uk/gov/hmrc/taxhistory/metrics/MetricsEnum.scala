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

package uk.gov.hmrc.taxhistory.metrics

import uk.gov.hmrc.taxhistory.metrics

object MetricsEnum extends Enumeration {

  type MetricsEnum = Value
  val NPS_GET_EMPLOYMENTS: metrics.MetricsEnum.Value = Value
  val RTI_GET_EMPLOYMENTS: metrics.MetricsEnum.Value = Value
  val NPS_GET_IABDS: metrics.MetricsEnum.Value       = Value
  val NPS_GET_TAX_ACCOUNT: metrics.MetricsEnum.Value = Value
  val CITIZEN_DETAILS: metrics.MetricsEnum.Value     = Value
}
