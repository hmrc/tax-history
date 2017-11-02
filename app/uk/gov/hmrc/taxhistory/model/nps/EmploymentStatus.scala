package uk.gov.hmrc.taxhistory.model.nps
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait EmploymentStatus

object EmploymentStatus {

  case object Live extends EmploymentStatus

  case object PotentiallyCeased extends EmploymentStatus

  case object Ceased extends EmploymentStatus

  case class Unknown(i: Int) extends EmploymentStatus

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "employmentStatus").read[Int].flatMap[EmploymentStatus] {
      case 1 => Reads(_ => JsSuccess(Live))
      case 2 => Reads(_ => JsSuccess(PotentiallyCeased))
      case 3 => Reads(_ => JsSuccess(Ceased))
      case _ => Reads(_ => JsError(JsPath \ "employmentStatus", ValidationError("Invalid EmploymentStatus")))
    }
  }

    implicit val jsonWrites = Writes[EmploymentStatus] {
      case Live => Json.obj("employmentStatus" -> 1)
      case PotentiallyCeased => Json.obj("employmentStatus" -> 2)
      case Ceased => Json.obj("employmentStatus" -> 3)
      case Unknown(status) => Json.obj("employmentStatus" -> status)
    }
}