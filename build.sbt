import Dependencies._

lazy val root =
  (project in file("."))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging)
    .settings(
      name := "weight-tracker-api",
      organization := "com.ruchij",
      scalaVersion := SCALA_VERSION,
      maintainer := "me@ruchij.com",
      libraryDependencies ++= rootDependencies ++ rootTestDependencies.map(_ % Test),
      buildInfoKeys := BuildInfoKey.ofN(name, organization, version, scalaVersion, sbtVersion),
      buildInfoPackage := "com.eed3si9n.ruchij",
      scalacOptions ++= Seq("-Ypartial-unification"),
      testOptions in Test +=
        Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-results"),
      addCompilerPlugin(kindProjector),
      addCompilerPlugin(betterMonadicFor)
    )

lazy val databaseMigration =
  (project in file("./database-migration"))
    .settings(
      name := "database-migration",
      organization := "com.ruchij",
      scalaVersion := SCALA_VERSION,
      maintainer := "me@ruchij.com",
      libraryDependencies ++= Seq(postgresql, flywayCore)
    )
    .dependsOn(root)

lazy val rootDependencies =
  Seq(
    http4sDsl,
    http4sBlazeServer,
    http4sCirce,
    circeGeneric,
    circeParser,
    pureconfig,
    jodaTime,
    jbcrypt,
    doobiePostgres,
    redisScala,
    enumeratum,
    logbackClassic,
    scalaLogging
  )

lazy val rootTestDependencies =
  Seq(scalaTest, pegdown)

addCommandAlias("testWithCoverage", "; coverage; test; coverageReport")
