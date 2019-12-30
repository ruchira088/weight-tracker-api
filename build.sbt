import Dependencies._

inThisBuild {
  Seq(
    organization := "com.ruchij",
    scalaVersion := SCALA_VERSION,
    maintainer := "me@ruchij.com",
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    topLevelDirectory := None,
    scalacOptions ++= Seq("-Ypartial-unification", "-Xlint", "-feature", "-deprecation"),
    resolvers += "confluent" at "https://packages.confluent.io/maven/"
  )
}

lazy val root =
  (project in file("."))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging, SbtTwirl)
    .settings(
      name := "weight-tracker-api",
      version := "0.0.1",
      libraryDependencies ++= rootDependencies ++ rootTestDependencies.map(_ % Test),
      buildInfoKeys := BuildInfoKey.ofN(name, organization, version, scalaVersion, sbtVersion),
      buildInfoPackage := "com.eed3si9n.ruchij",
      javaOptions in Test += s"-Dconfig.resource=application.test.conf",
      fork in Test := true,
      coverageExcludedPackages := "<empty>;com.ruchij.App;html.*;.*SendGridEmailService;.*ConsoleEmailService"
)
    .dependsOn(databaseMigration)

lazy val databaseMigration =
  (project in file("./database-migration"))
    .enablePlugins(JavaAppPackaging)
    .settings(
      name := "database-migration",
      version := "0.0.1",
      libraryDependencies ++= Seq(postgresql, flywayCore, catsEffect, pureconfig, h2)
    )

lazy val loadTest =
  (project in file("./gatling-load-test"))
    .enablePlugins(GatlingPlugin)
    .settings(
      name := "gatling-load-test",
      version := "0.0.1",
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
    akkaStreamKafka,
    kafkaAvroSerializer,
    avro4sCore,
    doobiePostgres,
    redisScala,
    enumeratum,
    shapeless,
    logbackClassic,
    scalaLogging,
    akkaSlf4j,
    commonsValidator,
    sendgrid,
    h2,
    embeddedRedis
  )

lazy val rootTestDependencies =
  Seq(scalaTest, circeLiteral, javaFaker, pegdown)

addCommandAlias("testWithCoverage", "; coverage; test; coverageReport")
