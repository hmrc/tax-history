package uk.gov.hmrc.taxhistory.model.api

import play.api.libs.json._

import scala.util.Try

sealed trait EmploymentPaymentType extends Product with Serializable {
  val name: String
}

object EmploymentPaymentType {
  case object OccupationalPension extends EmploymentPaymentType { val name = "OccupationalPension" }
  case object JobseekersAllowance extends EmploymentPaymentType { val name = "JobseekersAllowance" }
  case object IncapacityBenefit extends EmploymentPaymentType { val name = "IncapacityBenefit" }
  case object EmploymentAndSupportAllowance extends EmploymentPaymentType { val name = "EmploymentAndSupportAllowance" }
  case object StatePensionLumpSum extends EmploymentPaymentType { val name = "StatePensionLumpSum" }

  def apply(name: String): EmploymentPaymentType = name.trim match {
    case OccupationalPension.name => OccupationalPension
    case JobseekersAllowance.name => JobseekersAllowance
    case IncapacityBenefit.name => IncapacityBenefit
    case EmploymentAndSupportAllowance.name => EmploymentAndSupportAllowance
    case StatePensionLumpSum.name => StatePensionLumpSum
  }

  def unapply(paymentType: EmploymentPaymentType): Option[String] = Some(paymentType.name)

  private implicit val reads: Reads[EmploymentPaymentType] = new Reads[EmploymentPaymentType] {
    override def reads(json: JsValue): JsResult[EmploymentPaymentType] = json match {
      case JsString(value)  => Try(JsSuccess(EmploymentPaymentType(value))).getOrElse(JsError(s"Invalid EmploymentPaymentType $value"))
      case invalid => JsError(s"Invalid EmploymentPaymentType $invalid")
    }
  }

  private implicit val writes: Writes[EmploymentPaymentType] = new Writes[EmploymentPaymentType] {
    override def writes(o: EmploymentPaymentType): JsValue = JsString(o.name)
  }

  implicit val format: Format[EmploymentPaymentType] = Format[EmploymentPaymentType](reads, writes)
}
