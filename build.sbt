import Dependencies._

inThisBuild {
  Seq(
    organization := "com.ruchij",
    maintainer := "me@ruchij.com",
    scalaVersion := SCALA_VERSION,
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions ++= Seq("-Ypartial-unification", "-Xlint", "-feature", "-deprecation"),
    resolvers += "confluent" at "https://packages.confluent.io/maven/"
  )
}

lazy val root =
  (project in file("."))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging)
    .settings(
      name := "weight-tracker-api",
      version := "0.0.1",
      libraryDependencies ++= rootDependencies ++ rootTestDependencies.map(_ % Test),
      buildInfoKeys := BuildInfoKey.ofN(name, organization, version, scalaVersion, sbtVersion),
      buildInfoPackage := "com.eed3si9n.ruchij",
      topLevelDirectory := None,
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
      topLevelDirectory := None,
      libraryDependencies ++= Seq(postgresql, flywayCore, catsEffect, pureconfig, h2)
    )

lazy val loadTest =
  (project in file("./gatling-load-test"))
    .enablePlugins(GatlingPlugin)
    .settings(
      name := "gatling-load-test",
      version := "0.0.1",
      topLevelDirectory := None,
      libraryDependencies ++= Seq(gatlingTestFramework, gatlingCharts).map(_ % Test)
    )
    .dependsOn(root % "test->test")

lazy val emailService =
  (project in file("./email-service"))
    .enablePlugins(SbtTwirl, JavaAppPackaging)
    .settings(
      name := "email-service",
      version := "0.0.1",
      topLevelDirectory := None,
      libraryDependencies ++= Seq(sendgrid)
    )
    .dependsOn(root)

val rootDependencies =
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
    h2,
    embeddedRedis
  )

lazy val rootTestDependencies =
  Seq(scalaTest, circeLiteral, javaFaker, pegdown)

addCommandAlias("testWithCoverage", "; coverage; test; coverageReport")
