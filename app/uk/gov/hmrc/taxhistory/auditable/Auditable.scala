/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.model.audit.{DataEventAuditType, DataEventDetail, DataEventTransaction}

import scala.concurrent.{ExecutionContext, Future}

class Auditable @Inject()(config: AppConfig, val audit: AuditConnector)(implicit executionContext: ExecutionContext){

  // This only has side-effects, making a fire and forget call to an external system
  def sendDataEvent(transactionName: DataEventTransaction, path: String = "N/A",
                    tags: Map[String, String] = Map.empty[String, String],
                    detail: DataEventDetail, eventType: DataEventAuditType)
                   (implicit hc: HeaderCarrier): Future[Unit] =
    Future(audit.sendEvent(DataEvent(config.appName, auditType = eventType.toString,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName.toString, path) ++ tags,
      detail = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails(detail.detail.toSeq: _*))))

  // This only has side-effects, making a fire and forget sendDataEvent
  def sendDataEvents(transactionName: DataEventTransaction, path: String = "N/A",
                     tags: Map[String, String] = Map.empty[String, String],
                     details: Seq[DataEventDetail], eventType: DataEventAuditType)
                    (implicit hc: HeaderCarrier): Unit =
    details foreach (detail => sendDataEvent(transactionName, path, tags, detail, eventType))
}
