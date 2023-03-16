/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.taxhistory.model

import uk.gov.hmrc.domain.{Modulus23Check, SimpleObjectReads, SimpleObjectWrites, TaxIdentifier}

/*
 * This class was copied from "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.47.0-play-28"
 * The reason to copy it, is due to the library using scala 2.12 with no plan to migrate to scala 2.13 in the near future
 */
case class Arn(value: String) extends TaxIdentifier

object Arn {
  private val arnPattern = "^[A-Z]ARN[0-9]{7}$".r

  def isValid(arn: String): Boolean =
    arn match {
      case arnPattern(_*) => ArnCheck.isValid(arn)
      case _              => false
    }

  implicit val arnReads  = new SimpleObjectReads[Arn]("value", Arn.apply)
  implicit val arnWrites = new SimpleObjectWrites[Arn](_.value)
}

private object ArnCheck extends Modulus23Check {

  def isValid(arn: String): Boolean = {
    val suffix: String = arn.substring(1)
    calculateCheckCharacter(suffix) == arn.charAt(0)
  }
}
