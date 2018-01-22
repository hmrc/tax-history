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

import com.google.inject.AbstractModule
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.taxhistory.{MicroserviceAuditConnector, TaxHistoryAuthConnector, WSHttp}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.controllers.AllowanceController
import uk.gov.hmrc.taxhistory.services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{CoreGet, CorePost}
import uk.gov.hmrc.play.microservice.config.LoadAuditingConfig

class Module extends AbstractModule {

  // todo : as far as possible get rid of these instance bindings.  They are here to ease the transition
  // to injection from trait/object

  def configure() = {
//    //bind(classOf[Audit]).toInstance(new Audit(AppName.appName, MicroserviceAuditConnector))
//    bind(classOf[NpsConnector]).toInstance(NpsConnector)
//    bind(classOf[RtiConnector]).toInstance(RtiConnector)
//    bind(classOf[TaxHistoryCacheService]).toInstance(new ImplTaxHistoryCacheService())
//    bind(classOf[EmploymentHistoryService]).toInstance(EmploymentHistoryService)
////    bind(classOf[AllowanceController]).toInstance(new AllowanceController(employmentHistoryService = EmploymentHistoryService, authConnector = TaxHistoryAuthConnector))
//    //bind(classOf[AllowanceController]).to(classOf[AllowanceController])
//    bind(classOf[AuthConnector]).toInstance(new TaxHistoryAuthConnector())





//    object RtiConnector extends RtiConnector {
//      // $COVERAGE-OFF$
//      override val httpGet: CoreGet = WSHttp
//      override val httpPost: CorePost = WSHttp
//
//      lazy val serviceUrl: String = s"${baseUrl("rti-hod")}"
//      lazy val authorization: String = "Bearer " + getConfString("rti-hod.authorizationToken", "local")
//      lazy val environment: String = getConfString("rti-hod.env", "local")
//      lazy val originatorId: String = getConfString("rti-hod.originatorId", "local")
//      // $COVERAGE-ON$
//    }

//    object NpsConnector extends NpsConnector {
//      // $COVERAGE-OFF$
//      override val httpGet: CoreGet = WSHttp
//      override val httpPost: CorePost = WSHttp
//      lazy val path: String = config("nps-hod").getString("path").fold("")(p => p)
//      lazy val serviceUrl: String = s"${baseUrl("nps-hod")}$path"
//      lazy val originatorId: String = getConfString("nps-hod.originatorId", "HMRC_GDS")
//      // $COVERAGE-ON$
//    }

//    lazy val auditingConfig = LoadAuditingConfig(s"auditing")

//    class TaxHistoryAuthConnector @Inject()(val serviceUrl: String, http: WSHttp) extends PlayAuthConnector with ServicesConfig {
//      val serviceUrl = baseUrl("auth")
//      val http = WSHttp
//    }



  }
}