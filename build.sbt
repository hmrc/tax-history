import uk.gov.hmrc.DefaultBuildSettings.itSettings

ThisBuild / majorVersion := 3
ThisBuild / scalaVersion := "3.5.2"

lazy val microservice =
  Project("tax-history", file("."))
    .enablePlugins(PlayScala, SbtDistributablesPlugin)
    .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
    .settings(PlayKeys.playDefaultPort := 9997)
    .settings(CodeCoverageSettings.settings)
    .settings(routesImport ++= Seq("uk.gov.hmrc.taxhistory.binders.PathBinders._"))
    .settings(
      libraryDependencies ++= AppDependencies(),
      scalacOptions ++= List(
        "-feature",
        "-Wconf:src=routes/.*:s"
      )
    )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(itSettings())

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt it/Test/scalafmt")
