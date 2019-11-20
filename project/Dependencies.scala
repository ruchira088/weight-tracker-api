import sbt._

object Dependencies
{
  val SCALA_VERSION = "2.12.10"
  val HTTP4S_VERSION = "0.20.13"
  val CIRCE_VERSION = "0.12.3"

  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % HTTP4S_VERSION

  lazy val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % HTTP4S_VERSION

  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % HTTP4S_VERSION

  lazy val circeGeneric = "io.circe" %% "circe-generic" % CIRCE_VERSION

  lazy val circeParser = "io.circe" %% "circe-parser" % CIRCE_VERSION

  lazy val circeLiteral = "io.circe" %% "circe-literal" % CIRCE_VERSION

  lazy val jawnFs2 = "org.http4s" %% "jawn-fs2" % "0.15.0"

  lazy val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.12.1"

  lazy val jodaTime = "joda-time" % "joda-time" % "2.10.5"

  lazy val jbcrypt = "org.mindrot" % "jbcrypt" % "0.4"

  lazy val kindProjector = "org.typelevel" %% "kind-projector" % "0.10.3"

  lazy val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.1"

  lazy val doobiePostgres =  "org.tpolecat" %% "doobie-postgres" % "0.8.6"

  lazy val flywayCore = "org.flywaydb" % "flyway-core" % "6.0.6"

  lazy val postgresql = "org.postgresql" % "postgresql" % "42.2.8"

  lazy val h2 = "com.h2database" % "h2" % "1.4.199"

  lazy val redisScala = "com.github.etaty" %% "rediscala" % "1.9.0"

  lazy val enumeratum = "com.beachape" %% "enumeratum" % "1.5.13"

  lazy val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.0.0"

  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val commonsValidator = "commons-validator" % "commons-validator" % "1.6"

  lazy val sendgrid = "com.sendgrid" % "sendgrid-java" % "4.4.1"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"

  lazy val embeddedRedis = "com.github.kstyrc" % "embedded-redis" % "0.6"

  lazy val javaFaker = "com.github.javafaker" % "javafaker" % "1.0.1"

  lazy val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
}
