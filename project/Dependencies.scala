import sbt._

object Dependencies
{
  val SCALA_VERSION = "2.12.10"

  val HTTP4S_VERSION = "0.21.1"
  val CIRCE_VERSION = "0.13.0"

  lazy val scalaJava8Compat = "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"

  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % HTTP4S_VERSION

  lazy val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % HTTP4S_VERSION

  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % HTTP4S_VERSION

  lazy val circeGeneric = "io.circe" %% "circe-generic" % CIRCE_VERSION

  lazy val circeParser = "io.circe" %% "circe-parser" % CIRCE_VERSION

  lazy val circeLiteral = "io.circe" %% "circe-literal" % CIRCE_VERSION

  lazy val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.12.2"

  lazy val jodaTime = "joda-time" % "joda-time" % "2.10.5"

  lazy val jbcrypt = "org.mindrot" % "jbcrypt" % "0.4"

  lazy val akkaStreamKafka = "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.1"

  lazy val kafkaAvroSerializer = "io.confluent" % "kafka-avro-serializer" % "5.4.0"

  lazy val avro4sCore = "com.sksamuel.avro4s" %% "avro4s-core" % "3.0.6"

  lazy val kindProjector = "org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full

  lazy val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.1"

  lazy val doobiePostgres =  "org.tpolecat" %% "doobie-postgres" % "0.8.8"

  lazy val flywayCore = "org.flywaydb" % "flyway-core" % "6.0.6"

  lazy val postgresql = "org.postgresql" % "postgresql" % "42.2.9"

  lazy val h2 = "com.h2database" % "h2" % "1.4.199"

  lazy val redisScala = "com.github.etaty" %% "rediscala" % "1.9.0"

  lazy val enumeratum = "com.beachape" %% "enumeratum" % "1.5.15"

  lazy val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.0.0"

  lazy val awsS3 = "software.amazon.awssdk" % "s3" % "2.10.65"

  lazy val googleAuth = "com.warrenstrange" % "googleauth" % "1.4.0"

  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  lazy val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % "2.5.23"

  lazy val commonsValidator = "commons-validator" % "commons-validator" % "1.6"

  lazy val sendgrid = "com.sendgrid" % "sendgrid-java" % "4.4.1"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.0"

  lazy val embeddedRedis = "com.github.kstyrc" % "embedded-redis" % "0.6"

  lazy val javaFaker = "com.github.javafaker" % "javafaker" % "1.0.2"

  lazy val pegdown = "org.pegdown" % "pegdown" % "1.6.0"

  lazy val gatlingTestFramework = "io.gatling" % "gatling-test-framework" % "3.3.1"

  lazy val gatlingCharts = "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.3.1"
}
