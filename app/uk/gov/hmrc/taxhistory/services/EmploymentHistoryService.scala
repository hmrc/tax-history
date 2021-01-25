/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment}
import uk.gov.hmrc.taxhistory.auditable.Auditable
import uk.gov.hmrc.taxhistory.config.AppConfig
import uk.gov.hmrc.taxhistory.connectors.{DesNpsConnector, RtiConnector}
import uk.gov.hmrc.taxhistory.model.api.Employment._
import uk.gov.hmrc.taxhistory.model.api.FillerState._
import uk.gov.hmrc.taxhistory.model.api._
import uk.gov.hmrc.taxhistory.model.audit._
import uk.gov.hmrc.taxhistory.model.nps._
import uk.gov.hmrc.taxhistory.services.helpers.IabdsOps._
import uk.gov.hmrc.taxhistory.services.helpers.{EmploymentHistoryServiceHelper, EmploymentMatchingHelper}
import uk.gov.hmrc.taxhistory.utils.Logging
import uk.gov.hmrc.time.TaxYear

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class EmploymentHistoryService @Inject()(val desNpsConnector: DesNpsConnector,
                                         val rtiConnector: RtiConnector,
                                         val cacheService: PayeCacheService,
                                         val auditable: Auditable,
                                         config: AppConfig)
                                        (implicit executionContext: ExecutionContext)extends Logging {

  def getEmployments(nino: Nino, taxYear: TaxYear)(implicit headerCarrier: HeaderCarrier): Future[List[Employment]] = {
    getFromCache(nino, taxYear).map { es =>
      val employments = es.employments.map(_.enrichWithURIs(taxYear.startYear))

      if (employments.forall(_.isOccupationalPension)) {employments}
      else if (config.jobSeekersAllowanceFlag) {addFillers(employments, taxYear)}
      else {addFillers(employments, taxYear).filterNot(emp => emp.isJobseekersAllowance)}
    }
  }

  def addFillers(employments: List[Employment], taxYear: TaxYear): List[Employment] = {
    val employmentsWithoutPensions = employments.filterNot(emp => emp.isOccupationalPension)
    val employmentGapFiller = Employment.noRecord(taxYear.starts, taxYear.finishes)
    val fillers = getFillers(employmentsWithoutPensions, List(employmentGapFiller), taxYear)

    (employments ++ fillers) sortBy { employment =>
      employment.startDate.getOrElse {
        // This employment has no start date.
        // Let it be ordered at the start of all employments.
        taxYear.starts.minusDays(1)
      }.toDate
    }
  }

  @tailrec
  private def getFillers(employments: List[Employment], employmentGapFillers: List[Employment], taxYear: TaxYear): List[Employment] =
    if (employments.nonEmpty) {
      val dateAlignedFillers = employmentGapFillers flatMap (filler => alignFillerDates(filler, employments.head, taxYear))
      getFillers(employments.tail, dateAlignedFillers, taxYear)
    } else {
      employmentGapFillers
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
          if (config.statePensionFlag) {
            Some(statePension)
          } else {
            None
          })
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

    val completeTaxYearList = if (config.currentYearFlag) TaxYear.current +: taxYearList else taxYearList

    val taxYears = completeTaxYearList.map(year => IndividualTaxYear(year = year.startYear,
      allowancesURI = s"/${year.startYear}/allowances",
      employmentsURI = s"/${year.startYear}/employments",
      taxAccountURI = s"/${year.startYear}/tax-account"))
    println(config.currentYearFlag)
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
      rtiDataOpt <- retrieveRtiData(nino, taxYear)
      rtiEmployments = rtiDataOpt.map(_.employments).getOrElse(Nil)
      iabds <- retrieveNpsIabds(nino, taxYear)
      taxAccountOpt <- retrieveNpsTaxAccount(nino, taxYear)
      npsEmployments <- retrieveNpsEmployments(nino, taxYear)
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

    val employmentMatches: Map[NpsEmployment, RtiEmployment] = EmploymentMatchingHelper.matchEmployments(npsEmployments, rtiEmployments)

    val unMatchedNpsRecords = unmatchedEmployments(employmentMatches.keySet.toList, npsEmployments)
      .map(nps => DataEventDetail(auditNpsEventHelper(nino.value, nps)))

    val unMatchedRtiRecords = unmatchedEmployments(employmentMatches.values.toList, rtiEmployments)
      .map(rti => DataEventDetail(auditRtiEventHelper(nino.value, rti)))

    sendDataEvents(unMatchedNpsRecords, OnlyInNps)
    sendDataEvents(unMatchedRtiRecords, OnlyInRti)

    // One [[PayAsYouEarn]] instance will be produced for each npsEmployment.
    val payes: List[PayAsYouEarn] = npsEmployments.map { npsEmployment =>

      val matchedIncomeSource: Option[IncomeSource] = taxAccountOption.flatMap(_.matchedIncomeSource(npsEmployment))
      
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

      desNpsConnector.getEmployments(nino, passedInTaxYear).map { employments =>
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
    rtiConnector.getRTIEmployments(nino, taxYear)

  /*
    Retrieve Iabds directly from DES.
   */
  def retrieveNpsIabds(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[List[Iabd]] =
    desNpsConnector.getIabds(nino, taxYear.currentYear)

  /*
    Retrieve TaxAccount directly from DES.
   */
  def retrieveNpsTaxAccount(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Option[NpsTaxAccount]] =
    desNpsConnector.getTaxAccount(nino, taxYear.currentYear)

  private def unmatchedEmployments[A](matched: List[A], raw: List[A]): List[A] = {
    (raw.toSet -- matched.toSet).toList
  }

  private def auditRtiEventHelper(nino: String, rti: RtiEmployment): Map[String, String] =
    Map("nino" -> nino, "payeRef" -> rti.payeRef, "officeNumber" -> rti.officeNumber,
      "currentPayId" -> rti.currentPayId.getOrElse(""))

  private def auditNpsEventHelper(nino: String, nps: NpsEmployment): Map[String, String] =
    Map("nino" -> nino, "payeNumber" -> nps.payeNumber, "taxDistrictNumber" -> nps.taxDistrictNumber,
      "worksNumber" -> nps.worksNumber.getOrElse(""))

  private def sendDataEvents(records: List[DataEventDetail], auditType: DataEventAuditType)(implicit hc: HeaderCarrier) = {
    if(records.nonEmpty) auditable.sendDataEvents(transactionName = PAYEForAgents, details = records, eventType = auditType)
  }

  private def alignFillerDates(filler: Employment, employment: Employment, taxYear: TaxYear): List[Employment] = {
    val fillerEndDate = filler.endDate.getOrElse(taxYear.finishes)
    val fillerStartDate = filler.startDate.getOrElse(fillerEndDate)

    val assumedEmploymentStartDate = employment.startDate.getOrElse(employment.endDate.getOrElse(taxYear.starts))
    val assumedEmploymentEndDate = employment.endDate.getOrElse(taxYear.finishes)

    fillerState(fillerStartDate, fillerEndDate, assumedEmploymentStartDate, assumedEmploymentEndDate) match {
      case EncompassedByEmployment => // Discard the employment gap
        List.empty
      case OverlapEmployment => // Split the employment gap into two gaps, a gap before and a gap after the actual employment
        List(
          noRecord(fillerStartDate, assumedEmploymentStartDate.minusDays(1)),
          noRecord(employment.endDate.map(_.plusDays(1)).getOrElse(taxYear.finishes), fillerEndDate)
        )
      case OverlapEmploymentStart => // Align end date
        List(noRecord(fillerStartDate, assumedEmploymentStartDate.minusDays(1)))
      case OverlapEmploymentEnd => // Align start date
        List(noRecord(employment.endDate.map(_.plusDays(1)).getOrElse(taxYear.finishes), fillerEndDate))
      case _ => // Unchanged
        List(filler)
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