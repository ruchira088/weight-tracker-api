import Dependencies._

lazy val root =
  (project in file("."))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging, SbtTwirl)
    .settings(
      name := "weight-tracker-api",
      version := "0.0.1",
      organization := "com.ruchij",
      scalaVersion := SCALA_VERSION,
      maintainer := "me@ruchij.com",
      libraryDependencies ++= rootDependencies ++ rootTestDependencies.map(_ % Test),
      buildInfoKeys := BuildInfoKey.ofN(name, organization, version, scalaVersion, sbtVersion),
      buildInfoPackage := "com.eed3si9n.ruchij",
      scalacOptions ++= Seq("-Ypartial-unification", "-Xlint", "-feature", "-deprecation"),
      javaOptions in Test += s"-Dconfig.resource=application.test.conf",
      fork in Test := true,
      topLevelDirectory := None,
      addCompilerPlugin(kindProjector),
      addCompilerPlugin(betterMonadicFor),
      coverageExcludedPackages := "<empty>;com.ruchij.App;html.*;.*SendGridEmailService",
)
    .dependsOn(databaseMigration)

lazy val databaseMigration =
  (project in file("./database-migration"))
    .enablePlugins(JavaAppPackaging)
    .settings(
      name := "database-migration",
      version := "0.0.1",
      organization := "com.ruchij",
      scalaVersion := SCALA_VERSION,
      maintainer := "me@ruchij.com",
      libraryDependencies ++= Seq(postgresql, flywayCore, catsEffect, pureconfig, h2),
      topLevelDirectory := None
    )

lazy val loadTest =
  (project in file("./gatling-load-test"))
    .enablePlugins(GatlingPlugin)
    .settings(
      name := "gatling-load-test",
      version := "0.0.1",
      organization := "com.ruchij",
      scalaVersion := SCALA_VERSION,
      maintainer := "me@ruchij.com",
      addCompilerPlugin(kindProjector),
      libraryDependencies ++= Seq(gatlingTestFramework, gatlingCharts, pureconfig, catsEffect).map(_ % Test)
    )
    .dependsOn(root % "test->test")

lazy val rootDependencies =
  Seq(
    http4sDsl,
    http4sBlazeServer,
    http4sCirce,
    circeGeneric,
    circeParser,
    jawnFs2,
    pureconfig,
    jodaTime,
    jbcrypt,
    doobiePostgres,
    redisScala,
    enumeratum,
    shapeless,
    logbackClassic,
    scalaLogging,
    akkaSlf4j,
    commonsValidator,
    sendgrid
  )

lazy val rootTestDependencies =
  Seq(scalaTest, h2, circeLiteral, embeddedRedis, javaFaker, pegdown)

addCommandAlias("testWithCoverage", "; coverage; test; coverageReport")
