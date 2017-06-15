package uk.gov.hmrc.taxhistory.model.nps

import play.api.libs.json.Json


/**
  * Created by shailesh on 15/06/17.
  */


case class NpsEmployment(nino:String,
                      sequenceNumber:Int,
                      taxDistrictNumber:String,
                      payeNumber:String,
                      employerName:String,
                      worksNumber: Option[String]= None
                    )


object NpsEmployment {
  implicit val formats = Json.format[NpsEmployment]
}
