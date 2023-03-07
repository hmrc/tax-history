import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys.routesImport
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "tax-history"

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;modgiels/.data/..*;" +
      "uk.gov.hmrc.taxhistory.auditable;uk.gov.hmrc.taxhistory.metrics;view.*;controllers.auth.*;filters.*;forms.*;config.*;" +
      ".*BuildInfo.*;prod.Routes;app.Routes;testOnlyDoNotUseInAppConf.Routes;controllers.ExampleController;controllers.testonly.TestOnlyController",
    ScoverageKeys.coverageMinimumStmtTotal := 90.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(SbtAutoBuildPlugin, play.sbt.PlayScala, SbtDistributablesPlugin)
    .settings(PlayKeys.playDefaultPort := 9997)
    .settings(scoverageSettings: _*)
    .settings(scalaSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(routesImport ++= Seq("uk.gov.hmrc.taxhistory.binders.PathBinders._"))
    .settings(
      scalaVersion := "2.13.10",
      libraryDependencies ++= AppDependencies(),
      retrieveManaged := true,
      // ***************
      // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
      libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
      scalacOptions += "-Wconf:src=routes/.*:s"
    )
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      majorVersion := 3,
      IntegrationTest / Keys.fork := false,
      IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
      addTestReportOption(IntegrationTest, "int-test-reports"),
      IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
      IntegrationTest / parallelExecution := false
    )
    .settings(resolvers += Resolver.jcenterRepo)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] = tests map { test =>
  val forkOptions = ForkOptions().withRunJVMOptions(Vector("-Dtest.name=" + test.name))
  Group(test.name, Seq(test), SubProcess(config = forkOptions))
}

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt")
addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle")
