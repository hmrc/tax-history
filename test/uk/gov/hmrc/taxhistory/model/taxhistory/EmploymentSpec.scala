package uk.gov.hmrc.taxhistory.model.taxhistory

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.model.rti.RtiData
import uk.gov.hmrc.taxhistory.model.utils.TestUtil

class EmploymentSpec extends TestUtil with UnitSpec {

  lazy val employmentDetailsJson = loadFile("/json/taxhistory/employments.json")

  lazy val employments = List(
    Employment(
      payeReference = "paye-1",
      employerName = "employer-1",
      taxablePayTotal = BigDecimal.valueOf(123.12),
      taxablePayEYU = Some(BigDecimal.valueOf(-12.12)),
      taxTotal = BigDecimal.valueOf(14.14),
      taxEYU = Some(BigDecimal.valueOf(-1.14))),
    Employment(
      payeReference = "paye-2",
      employerName = "employer-2",
      taxablePayTotal = BigDecimal.valueOf(543.21),
      taxablePayEYU = Some(BigDecimal.valueOf(-21.00)),
      taxTotal = BigDecimal.valueOf(78.90),
      taxEYU = Some(BigDecimal.valueOf(-4.56)))
    )

  "Employment List" should {

    "transform into Json from object list correctly " in {
      Json.toJson(employments) shouldBe employmentDetailsJson
    }
    "transform into object list from json correctly " in {
      employmentDetailsJson.as[List[Employment]] shouldBe employments
    }
  }
}

