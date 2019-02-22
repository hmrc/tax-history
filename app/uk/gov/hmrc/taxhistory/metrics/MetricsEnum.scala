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

package uk.gov.hmrc.taxhistory.metrics


object MetricsEnum extends Enumeration {

  type MetricsEnum = Value
  val NPS_GET_EMPLOYMENTS = Value
  val RTI_GET_EMPLOYMENTS = Value
  val NPS_GET_IABDS = Value
  val NPS_GET_TAX_ACCOUNT = Value
  val CITIZEN_DETAILS = Value
  val DES_NPS_GET_EMPLOYMENTS = Value
}
