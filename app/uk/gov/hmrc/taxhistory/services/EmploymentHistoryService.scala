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

package uk.gov.hmrc.taxhistory.services


import javax.inject.Inject

import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.{GenericHttpError, NotFound, TaxHistoryException}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector
import uk.gov.hmrc.taxhistory.connectors.nps.NpsConnector
import uk.gov.hmrc.taxhistory.model.api._
import uk.gov.hmrc.taxhistory.model.nps.{NpsEmployment, _}
import uk.gov.hmrc.taxhistory.services.helpers.EmploymentHistoryServiceHelper
import uk.gov.hmrc.taxhistory.utils.TaxHistoryLogger
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class EmploymentHistoryService @Inject()(
                              val audit: Audit,
                              val npsConnector: NpsConnector,
                              val rtiConnector: RtiConnector,
                              val cacheService : TaxHistoryCacheService
                              ) extends EmploymentHistoryServiceHelper with Auditable with TaxHistoryLogger {

  def getEmployments(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[List[Employment]] = {
    getFromCache(nino, taxYear).flatMap { paye =>
      logger.warn("Returning js result from getEmployments")

      if (paye.employments.isEmpty) {
        // If no employments, return a not found error. This preserves the existing logic for now. TODO Review logic
        Future.failed(TaxHistoryException.notFound(classOf[Employment], (nino, taxYear)))
      } else {
        Future.successful(paye.employments.map(_.enrichWithURIs(taxYear.startYear)))
      }
    }
  }

  def getEmployment(nino: Nino, taxYear: TaxYear, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[Employment] = {
    getFromCache(nino, taxYear).flatMap { paye =>
      logger.warn("Returning js result of a getEmployment")
      paye.employments.find(_.employmentId.toString == employmentId) match {
        case Some(employment) =>
          Future.successful(employment.enrichWithURIs(taxYear.startYear))
        case None =>
          logger.warn("Cache has expired from mongo")
          Future.failed(TaxHistoryException.notFound(classOf[Employment], (nino, taxYear)))
      }
    }
  }


  def getFromCache(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[PayAsYouEarn] = {
    cacheService.getOrElseInsert(nino, taxYear) {
      retrieveEmploymentsDirectFromSource(nino, taxYear).map { h =>
        logger.warn(s"Refreshing cached data for $nino $taxYear")
        h
      }
    }
  }

  def getAllowances(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[List[Allowance]] = {
    getFromCache(nino, taxYear).flatMap(paye => {
      logger.warn("Returning js result from getAllowances")

      if (paye.allowances.isEmpty) {
        Future.failed(TaxHistoryException.notFound(classOf[Allowance], (nino, taxYear)))
      } else {
        Future.successful(paye.allowances)
      }
    })
  }


  def getPayAndTax(nino: Nino, taxYear: TaxYear, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[PayAndTax] = {
    getFromCache(nino, taxYear).flatMap { paye =>
      logger.warn("Returning js result from getEmployments")

      paye.payAndTax match {
        case None => Future.failed(TaxHistoryException.notFound(classOf[PayAndTax], (nino, taxYear, employmentId)))
        case Some(payAndTax) =>
          payAndTax.get(employmentId)
            .map(Future.successful(_))
            .getOrElse(Future.failed(TaxHistoryException.notFound(classOf[PayAndTax], (nino, taxYear, employmentId))))
      }
    }
  }

  def getTaxAccount(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[TaxAccount] = {
    getFromCache(nino, taxYear).flatMap { paye =>
      logger.warn("Returning js result from getTaxAccount")

      paye.taxAccount match {
        case Some(taxAccount) => Future.successful(taxAccount)
        case None => Future.failed(TaxHistoryException.notFound(classOf[TaxAccount], (nino, taxYear)))
      }
    }
  }

  def getTaxYears(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[List[IndividualTaxYear]] = {

    val taxYearList = List(TaxYear.current.back(1),
                           TaxYear.current.back(2),
                           TaxYear.current.back(3),
                           TaxYear.current.back(4))

    val taxYears = taxYearList.map(year => IndividualTaxYear(year = year.startYear,
                                                             allowancesURI = s"/${year.startYear}/allowances",
                                                             employmentsURI = s"/${year.startYear}/employments",
                                                             taxAccountURI = s"/${year.startYear}/tax-account"))

    Future.successful(taxYears)
  }

  def getCompanyBenefits(nino: Nino, taxYear: TaxYear, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[List[CompanyBenefit]] = {
    getFromCache(nino, taxYear).flatMap { paye =>
      logger.warn("Returning js result from getCompanyBenefits")

      paye.benefits match {
        case None => Future.failed(TaxHistoryException.notFound(classOf[CompanyBenefit], (nino, taxYear, employmentId)))
        case Some(companyBenefits) =>
          companyBenefits.get(employmentId)
            .map(Future.successful(_))
            .getOrElse(Future.failed(TaxHistoryException.notFound(classOf[CompanyBenefit], (nino, taxYear, employmentId))))
      }
    }
  }

  def retrieveEmploymentsDirectFromSource(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[PayAsYouEarn] = {

    for {
      employments <- getNpsEmployments(nino, taxYear)
      result      <-
        if (employments.isEmpty) {
          Future.failed(TaxHistoryException.notFound(classOf[PayAsYouEarn], (nino, taxYear)))
          // TODO this is to preserve existing logic. To review
        } else {
          mergeAndRetrieveEmployments(nino, taxYear)(employments)
        }
    } yield result
  }

  def mergeAndRetrieveEmployments(nino: Nino, taxYear: TaxYear)(npsEmployments: List[NpsEmployment])
                                 (implicit headerCarrier: HeaderCarrier): Future[PayAsYouEarn] = {
    for {
      iabdsF  <- getNpsIabds(nino,taxYear).map(Some(_)).recover { case TaxHistoryException(_, _) => None } // TODO this is done to preserve existing logic. Logic of 'combineResult' to be reviewed!
      rtiF    <- getRtiEmployments(nino,taxYear).map(Some(_)).recover { case TaxHistoryException(_, _) => None } // TODO this is done to preserve existing logic. Logic of 'combineResult' to be reviewed!
      taxAccF <- getNpsTaxAccount(nino,taxYear).map(Some(_)).recover { case TaxHistoryException(_, _) => None } // TODO this is done to preserve existing logic. Logic of 'combineResult' to be reviewed!
    } yield {
      combineResult(iabdsF,rtiF,taxAccF)(npsEmployments)
    }
  }

  def getNpsEmployments(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[List[NpsEmployment]] = {
    npsConnector.getEmployments(nino, taxYear.currentYear).map { employments =>
      employments.filterNot(x => x.receivingJobSeekersAllowance || x.otherIncomeSourceIndicator)
    }.recover {
      case TaxHistoryException(NotFound(_, _), _) => Nil // In case of a 404 don't fail but instead return an empty list
    }
  }

  def getRtiEmployments(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[RtiData] =
    rtiConnector.getRTIEmployments(nino, taxYear)

  def getNpsIabds(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[List[Iabd]] =
    npsConnector.getIabds(nino, taxYear.currentYear)

  def getNpsTaxAccount(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Option[NpsTaxAccount]] = {

    if (taxYear.startYear != TaxYear.current.previous.startYear) {
      Future.successful(None)
    } else {
      npsConnector.getTaxAccount(nino, taxYear.currentYear).map(Some(_))
    }
  }
}
