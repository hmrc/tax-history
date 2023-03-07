import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private lazy val hmrcBootstrapPlayVersion = "7.14.0"
  private lazy val hmrcMongoPlayVersion     = "1.1.0"

  private val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % hmrcMongoPlayVersion,
    "uk.gov.hmrc"       %% "domain"                    % "8.1.0-play-28",
    "uk.gov.hmrc"       %% "tax-year"                  % "3.0.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoPlayVersion,
    "org.scalatest"          %% "scalatest"               % "3.2.15",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0",
    "org.scalatestplus"      %% "mockito-4-6"             % "3.2.15.0",
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.62.2",
    "org.mockito"             % "mockito-core"            % "4.11.0" //upgrade to 5 when moving to Java 11
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID]      = compile ++ test
}
