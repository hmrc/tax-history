import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "tax-history"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "3.2.0",
    "uk.gov.hmrc" %% "play-url-binders" % "2.1.0",
    "uk.gov.hmrc" %% "domain" % "5.2.0",
    "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.10.0",
    "uk.gov.hmrc" %% "tax-year" % "0.4.0",
    "uk.gov.hmrc" %% "auth-client" % "2.6.0",
    "uk.gov.hmrc" %% "mongo-caching" % "5.4.0"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.0.0" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "uk.gov.hmrc" %% "reactivemongo-test" % "3.1.0" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
    "org.mockito" % "mockito-all" % "2.0.2-beta" % scope
  )
}