import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val compile = Seq(
    ws,
    "uk.gov.hmrc"         %% "bootstrap-backend-play-28" % "5.4.0",
    "uk.gov.hmrc"         %% "domain"                    % "6.1.0-play-28",
    "uk.gov.hmrc"         %% "agent-mtd-identifiers"     % "0.35.0-play-28",
    "uk.gov.hmrc"         %% "tax-year"                  % "1.1.0", // 1.2.0 moved to java.time instead of org.joda.time which we don't want
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-play-28"        % "0.54.0",
    "com.typesafe.play"   %% "play-json-joda"            % "2.9.2"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % "0.54.0",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0",
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0",
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.35.10",
    "org.mockito"             % "mockito-core"            % "4.3.1",
    "org.pegdown"             % "pegdown"                 % "1.6.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
