import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys.routesImport
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;modgiels/.data/..*;uk.gov.hmrc.taxhistory.auditable;uk.gov.hmrc.taxhistory.metrics;view.*;controllers.auth.*;filters.*;forms.*;config.*;" +
      ".*BuildInfo.*;prod.Routes;app.Routes;testOnlyDoNotUseInAppConf.Routes;controllers.ExampleController;controllers.testonly.TestOnlyController",
    ScoverageKeys.coverageMinimum := 70.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

val appName = "tax-history"

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-play-26"     % "2.1.0",
  "uk.gov.hmrc" %% "domain"                % "5.10.0-play-26",
  "uk.gov.hmrc" %% "agent-mtd-identifiers"  % "0.19.0-play-26",
  "uk.gov.hmrc" %% "tax-year"              % "1.1.0", // 1.2.0 moved to java.time instead of org.joda.time which we don't want
  "uk.gov.hmrc" %% "auth-client"           % "3.2.0-play-26",
  "uk.gov.hmrc" %% "simple-reactivemongo"  % "7.30.0-play-26",
  "uk.gov.hmrc" %% "mongo-caching"         % "6.15.0-play-26",
  "com.typesafe.play" %% "play-json-joda"  % "2.6.14"
)

def test(scope: String = "test,it") = Seq(
  "uk.gov.hmrc"            %% "hmrctest"           % "3.9.0-play-26"  % scope,
  "uk.gov.hmrc"            %% "reactivemongo-test" % "4.21.0-play-26" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3"          % scope,
  "org.mockito"            %  "mockito-core"       % "2.28.2"         % scope
)

lazy val appDependencies: Seq[ModuleID] = compile ++ test()

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(Seq(
      play.sbt.PlayScala,
      SbtAutoBuildPlugin,
      SbtGitVersioning,
      SbtDistributablesPlugin,
      SbtArtifactory): _*)
    .settings(PlayKeys.playDefaultPort := 9997)
    .settings(scoverageSettings: _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(routesImport ++= Seq("uk.gov.hmrc.taxhistory.binders.PathBinders._"))
    .settings(
      scalaVersion := "2.12.12",
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
    )
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      majorVersion := 3,
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false
    )
    .settings(resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo
    ))

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] = tests map { test =>
  Group(test.name, Seq(test), SubProcess(ForkOptions(
    runJVMOptions = Seq("-Dtest.name=" + test.name))
  ))
}
