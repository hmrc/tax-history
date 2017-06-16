package uk.gov.hmrc.taxhistory.connectors

import com.codahale.metrics.Timer
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
import uk.gov.hmrc.tai.model.rti.{RtiData, RtiEmployment, RtiPayment}
import uk.gov.hmrc.taxhistory.connectors.des.RtiConnector

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import org.mockito.Mockito.when
import org.mockito.Matchers.any


class RtiConnectorSpec extends PlaySpec with MockitoSugar {

  "RtiConnector" should {
    "have the rti basic url " when {
      "given a valid nino" in {
        createSUT.rtiBasicUrl(Nino("AA111111A")) mustBe "/test/rti/individual/payments/nino/AA111111"
      }
    }

    "have the Rti Path Url" when {
      "given a valid nino and path" in {
        createSUT.rtiPathUrl(Nino("AA111111A"), "path") mustBe "/test/rti/individual/payments/nino/AA111111/path"
      }
    }

    "have withoutSuffix nino" when {
      "given a valid nino" in {
        createSUT.withoutSuffix(Nino("AA111111A")) mustBe "AA111111"
      }
    }

    "have createHeader" in {
      val headers = createSUT.createHeader
      headers.extraHeaders mustBe List(("Environment", "env"), ("Authorization", "auth"), ("Gov-Uk-Originator-Id", "orgId"))
    }

    "have get RTI" when {
      "given a valid Nino and TaxYear" in {
        val sut = createSUT
        implicit val hc = HeaderCarrier()

        val fakePayment = RtiPayment(
                            paidOnDate = new LocalDate(2016,12,1),
                            taxablePayYTD = BigDecimal.valueOf(12000.00),
                            totalTaxYTD = BigDecimal.valueOf(100.00)
                          )
        val fakeEmployment = RtiEmployment(
                                currentPayId = Some("PAYID"),
                                officeNumber = "OFFICENO",
                                payeRef = "PAYEREF",
                                sequenceNo = 1,
                                payments = List(fakePayment),
                                endOfYearUpdates = Nil
                              )
        val fakeRtiData = RtiData(
                            nino = "AA000000A",
                            employments = List(fakeEmployment)
                            )
        val fakeResponse: HttpResponse = HttpResponse(200, Some(Json.toJson(fakeRtiData)))

        when(sut.httpGet.GET[HttpResponse](any[String])(any(), any())).thenReturn(Future.successful(fakeResponse))

        val resp = sut.getRTI(Nino("AA111111A"), 17)
        val rtiData = Await.result(resp, 5 seconds)

        rtiData mustBe Some(fakeRtiData)

      }
    }
  }

  private class SUT extends RtiConnector {
    override val serviceUrl: String = "/test"

    override val environment: String = "env"

    override val authorization: String = "auth"

    override val originatorId: String = "orgId"

    override val httpGet: HttpGet = mock[HttpGet]

    override lazy val httpPost: HttpPost = ???

    val mockTimerContext = mock[Timer.Context]
  }
  private def createSUT = new SUT

}


