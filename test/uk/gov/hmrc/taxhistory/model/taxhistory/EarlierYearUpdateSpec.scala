package uk.gov.hmrc.taxhistory.model.taxhistory

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsSuccess

class EarlierYearUpdateSpec extends PlaySpec {

  "EarlierYearUpdate" must {
    "serialise and de-serialise json successfully" in {

      val eyu = EarlierYearUpdate(BigDecimal(20.10), BigDecimal(10.00), LocalDate.now)

      EarlierYearUpdate.format.reads(EarlierYearUpdate.format.writes(eyu)) must be(JsSuccess(eyu))

    }
  }

}
