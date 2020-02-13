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

package uk.gov.hmrc.taxhistory.auditable

import javax.inject.Named

import com.google.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.taxhistory.model.audit.{DataEventAuditType, DataEventDetail, DataEventTransaction}

import scala.concurrent.Future

class Auditable @Inject()(@Named("appName") val applicationName: String, val audit: Audit){

  // This only has side-effects, making a fire and forget call to an external system
  def sendDataEvent(transactionName: DataEventTransaction, path: String = "N/A",
                    tags: Map[String, String] = Map.empty[String, String],
                    detail: DataEventDetail, eventType: DataEventAuditType)
                   (implicit hc: HeaderCarrier): Future[Unit] =
    Future(audit.sendDataEvent(DataEvent(applicationName, auditType = eventType.toString,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName.toString, path) ++ tags,
      detail = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails(detail.detail.toSeq: _*))))

  // This only has side-effects, making a fire and forget sendDataEvent
  def sendDataEvents(transactionName: DataEventTransaction, path: String = "N/A",
                     tags: Map[String, String] = Map.empty[String, String],
                     details: Seq[DataEventDetail], eventType: DataEventAuditType)
                    (implicit hc: HeaderCarrier): Unit =
    details foreach (detail => sendDataEvent(transactionName, path, tags, detail, eventType))
}
