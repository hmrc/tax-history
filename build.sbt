import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys.routesImport
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt.{ForkOptions, TestDefinition, _}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;modgiels/.data/..*;uk.gov.hmrc.taxhistory.auditable;uk.gov.hmrc.taxhistory.metrics;view.*;controllers.auth.*;filters.*;forms.*;config.*;" +
      ".*BuildInfo.*;prod.Routes;app.Routes;testOnlyDoNotUseInAppConf.Routes;controllers.ExampleController;controllers.testonly.TestOnlyController",
    ScoverageKeys.coverageMinimum := 90.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

val appName = "tax-history"
val silencerVersion = "1.7.0"

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(Seq(
      play.sbt.PlayScala,
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
      libraryDependencies ++= AppDependencies(),
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      // ***************
      // Use the silencer plugin to suppress warnings
      // You may turn it on for `views` too to suppress warnings from unused imports in compiled twirl templates, but this will hide other warnings.
      // silence all warnings on autogenerated files
      scalacOptions += "-P:silencer:pathFilters=target/.*",
      // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
      scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}",
      // Suppress warnings due to mongo dates using `$date` in their Json representation
      scalacOptions += "-P:silencer:globalFilters=possible missing interpolator: detected interpolated identifier `\\$date`",
      libraryDependencies ++= Seq(
        compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
        "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
      )
      // ***************
    )
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      majorVersion := 3,
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false
    )
    .settings( resolvers += Resolver.jcenterRepo )

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] = tests map { test =>
  val forkOptions = ForkOptions().withRunJVMOptions(Vector("-Dtest.name=" + test.name))
  Group(test.name, Seq(test), SubProcess(config = forkOptions))
}