/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.tai.connectors

import play.Logger
import play.api.http.Status
import play.api.libs.json.{Format, Writes}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import uk.gov.hmrc.tai.model.rti.RtiData

import scala.concurrent.Future

trait BaseConnector extends ServicesConfig {

  def httpGet: HttpGet
  def httpPost: HttpPost
  def originatorId: String
  val defaultVersion: Int = -1

  implicit val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }

  def getVersionFromHttpHeader(httpResponse: HttpResponse): Int = {
    val npsVersion: Int = httpResponse.header("ETag").map(_.toInt).getOrElse(defaultVersion)
    npsVersion
  }

  def extraNpsHeaders(hc: HeaderCarrier, version: Int, txId: String): HeaderCarrier = {
    hc.withExtraHeaders("ETag" -> version.toString, "X-TXID" -> txId, "Gov-Uk-Originator-Id" -> originatorId)
  }

  def basicNpsHeaders(hc: HeaderCarrier): HeaderCarrier = {
    hc.withExtraHeaders("Gov-Uk-Originator-Id" -> originatorId)
  }

  def getFromNps[A](url: String)(implicit hc: HeaderCarrier, formats: Format[A]): Future[(A, Int)] = {
    implicit val hc = basicNpsHeaders(HeaderCarrier())
    val futureResponse = httpGet.GET[HttpResponse](url)
    futureResponse.flatMap {
      httpResponse =>
        httpResponse.status match {
          case Status.OK => {
              Future.successful((httpResponse.json.as[A], getVersionFromHttpHeader(httpResponse)))
          }

          case Status.NOT_FOUND => {
            Logger.warn(s"NPSAPI - No DATA Found error returned from NPS with status $httpResponse.status and url $url")
            Future.failed(new NotFoundException(httpResponse.body))
          }

          case Status.INTERNAL_SERVER_ERROR => {
            Logger.warn(s"NPSAPI - Internal Server error returned from NPS with status $httpResponse.status and url $url")
            Future.failed(new InternalServerException(httpResponse.body))
          }

          case Status.BAD_REQUEST => {
            Logger.warn(s"NPSAPI - Bad request exception returned from NPS  with status $httpResponse.status and url $url")
            Future.failed(new BadRequestException(httpResponse.body))
          }

          case _ => {
            Logger.warn(s"NPSAPI - A Server error returned from NPS with status $httpResponse.status and url $url")
            Future.failed(new HttpException(httpResponse.body, httpResponse.status))
          }
        }
    }
  }

  def postToNps[A](url: String, postData: A)(implicit hc: HeaderCarrier, writes: Writes[A]): Future[HttpResponse] = {
    val futureResponse = httpPost.POST(url, postData)
    futureResponse.flatMap {
      httpResponse =>
        httpResponse.status match {
          case (Status.OK | Status.NO_CONTENT | Status.ACCEPTED) => {
            Future.successful(httpResponse)
          }
          case _ => {
            Logger.warn(s"NPSAPI - A server error returned from NPS HODS in postToNps with status " +
              httpResponse.status + " url " + url)
            Future.failed(new HttpException(httpResponse.body, httpResponse.status))
          }
        }
     }
  }

  def getFromRTIWithStatus(url: String, reqNino: String)(implicit hc: HeaderCarrier): Future[Option[RtiData]] = {
    val futureResponse = httpGet.GET[HttpResponse](url)
    futureResponse.flatMap {
      res =>
        res.status match {
          case Status.OK => {

            val rtiData = res.json.as[RtiData](RtiData.reader)
            if (reqNino != rtiData.nino) {
              Logger.warn(s"RTIAPI - Incorrect Payload returned from RTI HODS for $reqNino")
              Future.successful((None))
            } else {
              Future.successful((Some(rtiData)))
            }
          }
          case Status.BAD_REQUEST => {
            val errorMessage = s"RTIAPI - Bad Request error returned from RTI HODS for $reqNino"
            Logger.warn(errorMessage)
            Future.failed(new BadRequestException(errorMessage))
          }
          case Status.NOT_FOUND => {
            val errorMessage =s"RTIAPI - No DATA Found error returned from RTI HODS for $reqNino"
            Logger.warn(errorMessage)
            Future.failed(new NotFoundException(errorMessage))
          }
          case Status.INTERNAL_SERVER_ERROR => {
            val errorMessage =s"RTIAPI - Internal Server error returned from RTI HODS $reqNino"
            Logger.warn(errorMessage)
            Future.failed(new InternalServerException(errorMessage))
          }
          case status => {
            val errorMessage =s"RTIAPI - An error returned from RTI HODS for $reqNino with status $status"
            Logger.warn(errorMessage)
            Future.failed(new ServiceUnavailableException(errorMessage))
          }
        }
    }
  }
}
