import uk.gov.hmrc.time.TaxYear

val x =TaxYear.current.previous
x.currentYear -1
val y = TaxYear(2015)
y.currentYear

