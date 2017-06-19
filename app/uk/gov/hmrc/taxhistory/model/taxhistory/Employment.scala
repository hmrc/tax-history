package uk.gov.hmrc.taxhistory.model.taxhistory

import play.api.libs.json.Json

case class Employment(payeReference:String,
                      employerName:String,
                      taxablePayTotal:BigDecimal,
                      taxTotal:BigDecimal,
                      taxablePayEYU:Option[BigDecimal] = None,
                      taxEYU:Option[BigDecimal] = None)

object Employment {
  implicit val formats = Json.format[Employment]
}
