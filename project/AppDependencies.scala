import sbt.*

object AppDependencies {

  private lazy val hmrcBootstrapPlayVersion = "10.2.0"
  private lazy val hmrcMongoPlayVersion     = "2.10.0"

  private val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % hmrcMongoPlayVersion,
    "uk.gov.hmrc"       %% "domain-play-30"            % "11.0.0",
    "uk.gov.hmrc"       %% "tax-year"                  % "6.0.0",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"     % "2.2.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % hmrcBootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoPlayVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID]      = compile ++ test

}
