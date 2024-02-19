import uk.gov.hmrc.DefaultBuildSettings.itSettings

val appName = "tax-history"

ThisBuild / majorVersion := 3
ThisBuild / scalaVersion := "2.13.12"

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(PlayScala, SbtDistributablesPlugin)
    .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
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
  .settings(libraryDependencies ++= AppDependencies.itDependencies)

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt it/Test/scalafmt")
addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle it/Test/scalastyle")
