package uk.gov.hmrc.taxhistory.model.nps

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.taxhistory.model.nps.EmploymentStatus.Live

class EmploymentStatusSpec extends UnitSpec {

  "EmploymentStatus" must {
    "read and write json successfully" in {
      EmploymentStatus.jsonReads.reads(EmploymentStatus.jsonWrites.writes(EmploymentStatus.Live)) shouldBe JsSuccess(Live)
      EmploymentStatus.jsonReads.reads(EmploymentStatus.jsonWrites.writes(EmploymentStatus.Ceased)) shouldBe JsSuccess(Live)
      EmploymentStatus.jsonReads.reads(EmploymentStatus.jsonWrites.writes(EmploymentStatus.PotentiallyCeased)) shouldBe JsSuccess(Live)
    }

    "throw error on invalid data" in {
      EmploymentStatus.jsonReads.reads(Json.obj("employmentStatus" -> 10)) shouldBe JsError(List((JsPath  \"employmentStatus",
        List(ValidationError(List("Invalid EmploymentStatus"))))))
    }
  }

}
