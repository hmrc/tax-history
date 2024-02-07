resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)

// To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
// Try to remove when sbt 1.8.0+ and scoverage is 2.0.7+
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"        % "3.20.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"         % "2.0.9")
addSbtPlugin("org.scalastyle"    % "scalastyle-sbt-plugin" % "1.0.0" exclude ("org.scala-lang.modules", "scala-xml_2.12"))
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables"    % "2.2.0")
addSbtPlugin("org.playframework" % "sbt-plugin"            % "3.0.1")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"           % "0.6.4")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"          % "2.5.2")
