import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private lazy val hmrcBootstrapPlayVersion = "8.4.0"
  private lazy val hmrcMongoPlayVersion     = "1.7.0"

  private val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % hmrcMongoPlayVersion,
    "uk.gov.hmrc"       %% "domain-play-30"            % "9.0.0",
    "uk.gov.hmrc"       %% "tax-year"                  % "4.0.0",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"     % "1.15.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoPlayVersion,
    "org.scalatest"     %% "scalatest"               % "3.2.17",
    "org.scalatestplus" %% "mockito-4-11"            % "3.2.17.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID]      = compile ++ test
}
