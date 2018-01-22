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

package uk.gov.hmrc.taxhistory.model.audit

trait DataEventAuditType
case object OnlyInRti extends DataEventAuditType
case object NpsRtiMismatch extends DataEventAuditType
case object AgentViewedClientEvent extends DataEventAuditType

trait DataEventTransaction

case object PAYEForAgents extends DataEventTransaction
case object AgentViewedClient extends DataEventTransaction


// todo : look at the naming of this, in use we end up with detail.detail
case class DataEventDetail (detail: Map[String, String])