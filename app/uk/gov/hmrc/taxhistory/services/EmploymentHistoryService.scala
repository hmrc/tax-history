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


import javax.inject.{Inject, Named}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.connectors.{DesNpsConnector, RtiConnector, SquidNpsConnector}
import uk.gov.hmrc.taxhistory.model.api.Employment._
import uk.gov.hmrc.taxhistory.model.api.FillerState._
import uk.gov.hmrc.taxhistory.model.api._
import uk.gov.hmrc.taxhistory.model.audit.{DataEventDetail, NpsRtiMismatch, OnlyInRti, PAYEForAgents}
import uk.gov.hmrc.taxhistory.model.nps._
import uk.gov.hmrc.taxhistory.services.helpers.IabdsOps._
import uk.gov.hmrc.taxhistory.services.helpers.{EmploymentHistoryServiceHelper, EmploymentMatchingHelper}
import uk.gov.hmrc.taxhistory.utils.Logging
import uk.gov.hmrc.time.TaxYear

import scala.annotation.tailrec
import scala.concurrent.Future

class EmploymentHistoryService @Inject()(val desNpsConnector: DesNpsConnector,
                                         val squidNpsConnector: SquidNpsConnector,
                                         val rtiConnector: RtiConnector,
                                         val cacheService: PayeCacheService,
                                         val auditable: Auditable,
                                         @Named("featureFlags.currentYearFlag") val currentYearFlag: Boolean,
                                         @Named("featureFlags.statePensionFlag") val statePensionFlag: Boolean,
                                         @Named("featureFlags.jobSeekersAllowanceFlag") val jobSeekersAllowanceFlag: Boolean
                                        ) extends Logging {

  def getEmployments(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[List[Employment]] = {
    getFromCache(nino, taxYear).map { es =>
      val employments = es.employments.map(_.enrichWithURIs(taxYear.startYear))

      if(employments.forall(_.isOccupationalPension)) employments
      else if (jobSeekersAllowanceFlag) addFillers(employments, taxYear)
      else addFillers(employments, taxYear).filterNot(emp => emp.isJobseekersAllowance)
    }
  }

  def addFillers(employments: List[Employment], taxYear: TaxYear): List[Employment] =
    (employments ++ getFillers(employments.filterNot(emp => emp.isOccupationalPension),
      List(Employment.noRecord(taxYear.starts, Some(taxYear.finishes))), taxYear)) sortBy (_.startDate.toDate)

  @tailrec
  private def getFillers(employments: List[Employment], fillers: List[Employment], taxYear: TaxYear): List[Employment] =
    if (employments.nonEmpty) {
      getFillers(employments.tail, fillers flatMap (filler => alignFillerDates(filler, employments.head, taxYear)), taxYear)
    } else {
      fillers
    }

  def getEmployment(nino: Nino, taxYear: TaxYear, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[Employment] = {
    getFromCache(nino, taxYear).flatMap { paye =>
      logger.debug("Returning result of a getEmployment")
      paye.employments.find(_.employmentId.toString == employmentId) match {
        case Some(employment) => Future.successful(employment.enrichWithURIs(taxYear.startYear))
        case None =>
          logger.info("Cache has expired from mongo")
          Future.failed(new NotFoundException(s"Employment not found for NINO ${nino.value} and tax year ${taxYear.toString}"))
      }
    }
  }


  def getFromCache(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[PayAsYouEarn] = {
    cacheService.getOrElseInsert(nino, taxYear) {
      retrieveAndBuildPaye(nino, taxYear).map { h =>
        logger.debug(s"Refreshing cached data for $nino $taxYear")
        h
      }
    }
  }

  def getAllowances(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[List[Allowance]] = {
    if (taxYear == TaxYear.current) {
      Future(List.empty[Allowance])
    }
    else {
      getFromCache(nino, taxYear).flatMap(paye => {
        logger.debug("Returning result from getAllowances")

        if (paye.allowances.isEmpty) {
          Future.failed(new NotFoundException(s"Allowance not found for NINO ${nino.value} and tax year ${taxYear.toString}"))
        } else {
          Future.successful(paye.allowances)
        }
      })
    }
  }


  def getPayAndTax(nino: Nino, taxYear: TaxYear, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[Option[PayAndTax]] = {
    getFromCache(nino, taxYear).map(_.payAndTax.get(employmentId))
      .orNotFound(s"PayAndTax not found for NINO ${nino.value}, tax year ${taxYear.toString} and employmentId $employmentId")
  }

  def getAllPayAndTax(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[Map[String, PayAndTax]] = {
    getFromCache(nino, taxYear).map(_.payAndTax)
      .orNotFound(s"PayAndTax not found for NINO ${nino.value}, tax year ${taxYear.toString}")
  }

  def getTaxAccount(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[Option[TaxAccount]] = {
    if (taxYear == TaxYear.current.previous) {
      getFromCache(nino, taxYear).flatMap { paye =>
        logger.debug("Returning result from getTaxAccount")

        paye.taxAccount match {
          case Some(taxAccount) => Future.successful(Some(taxAccount))
          case None => Future.failed(new NotFoundException(s"TaxAccount not found for NINO ${nino.value} and tax year ${taxYear.toString}"))
        }
      }
    } else {
      Future.failed(new NotFoundException(s"TaxAccount only available for last completed tax year"))
    }
  }

  def getStatePension(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[Option[StatePension]] = {
    getFromCache(nino, taxYear).flatMap { paye =>
      logger.debug("Returning result from getStatePension")

      paye.statePension match {
        case Some(statePension) => Future.successful(
          if (statePensionFlag) Some(statePension)
          else None)
        case None => Future.failed(new NotFoundException(s"StatePension not found for NINO ${nino.value} and tax year ${taxYear.toString}"))
      }
    }
  }

  def getIncomeSource(nino: Nino, taxYear: TaxYear, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[Option[IncomeSource]] = {
    (if (taxYear == TaxYear.current) {
      getFromCache(nino, taxYear).map(_.incomeSources.get(employmentId))
    } else {
      Future(None)
    }).orNotFound(s"IncomeSource not found for NINO ${nino.value}, tax year ${taxYear.toString}, and employmentId $employmentId")
  }

  def getTaxYears(nino: Nino): Future[List[IndividualTaxYear]] = {

    val taxYearList: List[TaxYear] = List(TaxYear.current.back(1),
      TaxYear.current.back(2),
      TaxYear.current.back(3),
      TaxYear.current.back(4))

    val completeTaxYearList = if (currentYearFlag) TaxYear.current +: taxYearList else taxYearList

    val taxYears = completeTaxYearList.map(year => IndividualTaxYear(year = year.startYear,
      allowancesURI = s"/${year.startYear}/allowances",
      employmentsURI = s"/${year.startYear}/employments",
      taxAccountURI = s"/${year.startYear}/tax-account"))

    Future.successful(taxYears)
  }

  def getCompanyBenefits(nino: Nino, taxYear: TaxYear, employmentId: String)(implicit headerCarrier: HeaderCarrier): Future[List[CompanyBenefit]] = {
    if (taxYear == TaxYear.current) {
      Future(List.empty[CompanyBenefit])
    }
    else {
      getFromCache(nino, taxYear).map(_.benefits.getOrElse(employmentId, List.empty))
        .orNotFound(s"CompanyBenefits not found for NINO ${nino.value}, tax year ${taxYear.toString} and employmentId $employmentId")
    }
  }

  /**
    * Retrieves (from connected microservices) the data required to build instances of
    * the tax year summary (`PayAsYouEarn`) and combines this data into a `PayAsYouEarn` instance.
    */
  def retrieveAndBuildPaye(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[PayAsYouEarn] = {
    val paye = for {
      npsEmployments <- retrieveNpsEmployments(nino, taxYear)
      rtiDataOpt <- retrieveRtiData(nino, taxYear)
      rtiEmployments = rtiDataOpt.map(_.employments).getOrElse(Nil)
      iabds <- retrieveNpsIabds(nino, taxYear)
      taxAccountOpt <- retrieveNpsTaxAccount(nino, taxYear)
    } yield {
      mergeEmployments(nino, taxYear, npsEmployments, rtiEmployments, taxAccountOpt, iabds)
    }
    paye
  }

  /**
    * Given the data required, performs the necessary logic to combine this data from
    * different sources into a `PayAsYouEarn` (tax year summary) instance.
    */
  def mergeEmployments(nino: Nino,
                       taxYear: TaxYear,
                       npsEmployments: List[NpsEmployment],
                       rtiEmployments: List[RtiEmployment],
                       taxAccountOption: Option[NpsTaxAccount],
                       iabds: List[Iabd]
                      )(implicit hc: HeaderCarrier): PayAsYouEarn = {

    val (iabdsNoStatePensions, statePension) = filterStatePension(iabds)

    val employmentMatches: Map[NpsEmployment, RtiEmployment] = EmploymentMatchingHelper.matchedEmployments(npsEmployments, rtiEmployments)

    // Check for any RTI employment which doesn't match any NPS employment, and send an audit event if this is the case.
    val onlyInRti = EmploymentMatchingHelper.unmatchedRtiEmployments(npsEmployments, rtiEmployments)
    if (onlyInRti.nonEmpty) {
      auditable.sendDataEvents(
        transactionName = PAYEForAgents,
        details = buildEmploymentDataEventDetails(nino.value, onlyInRti),
        eventType = OnlyInRti)
    }

    // Send an audit event for each employment that we weren't able to match conclusively.
    EmploymentMatchingHelper.ambiguousEmploymentMatches(npsEmployments, rtiEmployments).foreach { case (nps, rti) =>
      logger.info(s"Some NPS employments have multiple matching RTI employments.")
      auditable.sendDataEvents(
        transactionName = PAYEForAgents,
        details = buildEmploymentDataEventDetails(nino.value, rti),
        eventType = NpsRtiMismatch
      )
    }

    // One [[PayAsYouEarn]] instance will be produced for each npsEmployment.
    val payes: List[PayAsYouEarn] = npsEmployments.map { npsEmployment =>

      val matchedIncomeSource = taxAccountOption.flatMap(_.matchedIncomeSource(npsEmployment))
      
      if(matchedIncomeSource.isEmpty && taxYear == TaxYear.current) {
        logger.warn("No matched income source found for employment in current tax year")
      }

      EmploymentHistoryServiceHelper.buildPAYE(
        rtiEmployment = employmentMatches.get(npsEmployment),
        iabds = iabdsNoStatePensions.matchedCompanyBenefits(npsEmployment),
        incomeSource = matchedIncomeSource,
        npsEmployment = npsEmployment
      )
    }

    EmploymentHistoryServiceHelper.combinePAYEs(payes).copy(
      allowances = iabdsNoStatePensions.allowances,
      taxAccount = taxAccountOption.map(_.toTaxAccount),
      statePension = statePension.map(_.toStatePension))
  }

  /*
    Retrieve NpsEmployments directly from the NPS microservice.
   */
  def retrieveNpsEmployments(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[List[NpsEmployment]] = {
    val passedInTaxYear = taxYear.currentYear

    squidNpsConnector.getEmployments(nino, passedInTaxYear).map { employments =>
      if (TaxYear.current.currentYear.equals(passedInTaxYear)) {
        employments.filterNot(x => x.receivingJobSeekersAllowance | x.otherIncomeSourceIndicator)
      } else {
        employments.filterNot(_.otherIncomeSourceIndicator)
      }
    }.orNotFound(s"No NPS employments found for $nino $taxYear")
  }

  /*
    Retrieve RtiData directly from the RTI microservice.
   */
  def retrieveRtiData(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Option[RtiData]] =
    rtiConnector.getRTIEmployments(nino, taxYear).map(Some(_))
      .recover { case _ => None } // We want to present some information even if the retrieval from RTI failed.

  /*
    Retrieve Iabds directly from DES.
   */
  def retrieveNpsIabds(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[List[Iabd]] =
    desNpsConnector.getIabds(nino, taxYear.currentYear)
      .recover { case _ => Nil } // We want to present some information even if the retrieval of IABDs failed.

  /*
    Retrieve TaxAccount directly from DES.
   */
  def retrieveNpsTaxAccount(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Option[NpsTaxAccount]] =
    desNpsConnector.getTaxAccount(nino, taxYear.currentYear).map(Some(_))
      .recover { case _ => None } // We want to present some information even if the retrieval of the tax account failed.

  /*
   A convenience method used when auditing.
   */
  private def buildEmploymentDataEventDetails(nino: String, rtiEmployments: List[RtiEmployment]): Seq[DataEventDetail] =
    rtiEmployments.map(rE =>
      DataEventDetail(
        Map("nino" -> nino, "payeRef" -> rE.payeRef, "officeNumber" -> rE.officeNumber, "currentPayId" -> rE.currentPayId.getOrElse("")))
    )

  private def getFiller(previous: Option[Employment], nextE: Employment, taxYear: TaxYear): List[Employment] = {
    previous match {
      case Some(prevE) if prevE.endDate.getOrElse(taxYear.finishes).plusDays(1).isBefore(nextE.startDate) =>
        val fillerStartDate = prevE.endDate.getOrElse(taxYear.finishes).plusDays(1)
        val fillerEndDate = if (nextE.startDate.minusDays(1) == taxYear.finishes && taxYear == TaxYear.current) None else Some(nextE.startDate.minusDays(1))
        List(Employment.noRecord(fillerStartDate, fillerEndDate), nextE) // gap
      case _ => List(nextE) // no gap
    }
  }

  private def alignFillerDates(filler: Employment, employment: Employment, taxYear: TaxYear): List[Employment] = {
    fillerState(filler, employment, taxYear) match {
      case EncompassedByEmployment => List.empty // Discard
      case OverlapEmployment => List(noRecord(filler.startDate, Some(employment.startDate.minusDays(1))),
        noRecord(employment.endDate.getOrElse(taxYear.finishes.minusDays(1)).plusDays(1), filler.endDate)) // Split into two
      case OverlapEmploymentStart => List(noRecord(filler.startDate, Some(employment.startDate.minusDays(1)))) // Align end date
      case OverlapEmploymentEnd => List(noRecord(employment.endDate.getOrElse(taxYear.finishes.minusDays(1)).plusDays(1), filler.endDate)) // Align start date
      case _ => List(filler) // Unchanged
    }
  }

  private def filterStatePension(iabds: List[Iabd]): (List[Iabd], Option[Iabd]) = {
    def matchStatePension(iabd: Iabd): Boolean = {
      iabd.`type` match {
        case StatePensions => true
        case _ => false
      }
    }

    (iabds.filterNot(matchStatePension),
      iabds.find(matchStatePension))
  }
}