
import uk.gov.hmrc.DefaultBuildSettings

val appName = "tax-history"

ThisBuild / majorVersion := 3
ThisBuild / scalaVersion := "2.13.12"

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
    .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
    //.settings(majorVersion := 3)
    .settings(PlayKeys.playDefaultPort := 9997)
    .settings(CodeCoverageSettings.settings)
    .settings(routesImport ++= Seq("uk.gov.hmrc.taxhistory.binders.PathBinders._"))
    .configs(IntegrationTest)
    .settings(
      //scalaVersion := "2.13.12",
      libraryDependencies ++= AppDependencies(),
      retrieveManaged := true,
      // ***************
      // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
      libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
      scalacOptions ++= List(
        "-feature",
        "-Wconf:src=routes/.*:s"
      )
    )
    .settings(DefaultBuildSettings.integrationTestSettings())

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt IntegrationTest/scalafmt")
addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle IntegrationTest/scalastyle")
