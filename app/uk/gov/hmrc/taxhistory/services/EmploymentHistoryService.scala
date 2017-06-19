package uk.gov.hmrc.taxhistory.services

import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.EmploymentsConnector
import uk.gov.hmrc.taxhistory.model.nps.NpsEmployment
import uk.gov.hmrc.taxhistory.model.taxhistory.Employment
import uk.gov.hmrc.time.TaxYear

object EmploymentHistoryService extends EmploymentHistoryService

trait EmploymentHistoryService {
  def employmentsConnector : EmploymentsConnector = EmploymentsConnector
  def rtiConnector : RtiConnector = RtiConnector

  def getEmploymentHistory(nino:String, taxYear:TaxYear): List[Employment] = ???

  def getNpsEmployments(nino:String, taxYear:TaxYear): List[NpsEmployment] = ???

  def getRtiEmployments(nino:String, taxYear:TaxYear): List[RtiData] = ???

}
