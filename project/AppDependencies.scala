import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private lazy val hmrcBootstrapPlayVersion = "7.23.0"
  private lazy val hmrcMongoPlayVersion     = "1.3.0"

  private val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % hmrcMongoPlayVersion,
    "uk.gov.hmrc"       %% "domain"                    % "8.3.0-play-28",
    "uk.gov.hmrc"       %% "tax-year"                  % "4.0.0",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"     % "1.15.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28"  % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % hmrcMongoPlayVersion,
    "org.scalatest"       %% "scalatest"               % "3.2.17",
    "org.scalatestplus"   %% "mockito-4-11"            % "3.2.17.0",
    "com.vladsch.flexmark" % "flexmark-all"            % "0.64.8"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID]      = compile ++ test
}
