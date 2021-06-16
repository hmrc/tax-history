import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-26"   % "5.4.0",
    "uk.gov.hmrc"       %% "domain"                      % "5.11.0-play-26",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"       % "0.24.0-play-26",
    "uk.gov.hmrc"       %% "tax-year"                    % "1.1.0", // 1.2.0 moved to java.time instead of org.joda.time which we don't want
    "uk.gov.hmrc"       %% "simple-reactivemongo"        % "8.0.0-play-26",
    "uk.gov.hmrc"       %% "mongo-caching"               % "7.0.0-play-26",
    "com.typesafe.play" %% "play-json-joda"              % "2.6.14"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc"            %% "hmrctest"           % "3.10.0-play-26" % scope,
    "uk.gov.hmrc"            %% "reactivemongo-test" % "4.21.0-play-26" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3"          % scope,
    "org.mockito"            %  "mockito-core"       % "2.28.2"         % scope,
    "org.pegdown"            %  "pegdown"            % "1.6.0"          % scope
  )

  val all: Seq[ModuleID] = compile ++ test()
}
