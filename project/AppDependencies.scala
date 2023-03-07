import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private lazy val hmrcMongoPlayVersion = "0.71.0"

  //TODO migrate from jodaTime to JavaTime
  private val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % "7.2.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % hmrcMongoPlayVersion,
    "uk.gov.hmrc"       %% "domain"                    % "8.1.0-play-28",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"     % "0.58.0-play-28", //note that this doesnt exist with scala 2.13
    "uk.gov.hmrc"       %% "tax-year"                  % "3.0.0", // 1.2.0 moved to java.time instead of org.joda.time which we don't want
    "com.typesafe.play" %% "play-json-joda"            % "2.9.4",
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoPlayVersion,
    "org.scalatest"          %% "scalatest"               % "3.2.13",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0",
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0",
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.62.2",
    "org.mockito"             % "mockito-core"            % "5.1.1"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID]      = compile ++ test
}
