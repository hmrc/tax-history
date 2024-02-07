/*
 * Copyright 2004 HM Revenue & Customs
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

import org.mockito.Mockito
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.taxhistory.services.RelationshipAuthService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * A test version of RelationshipAuthService which reads off the Nino-Agent relationships from the given Map
  * rather than interrogating a real auth service.
  */
case class TestRelationshipAuthService(relationships: Map[Nino, Arn])
    extends RelationshipAuthService(authConnector = Mockito.mock(classOf[AuthConnector])) {

  override def retrieveArnFor(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[Arn]] =
    Future.successful(relationships.get(nino))

}
