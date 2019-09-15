import sbt._

object Dependencies
{
  val SCALA_VERSION = "2.12.10"
  val HTTP4S_VERSION = "0.20.10"

  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % HTTP4S_VERSION

  lazy val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % HTTP4S_VERSION

  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % HTTP4S_VERSION

  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.11.1"

  lazy val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.11.1"

  lazy val jodaTime = "joda-time" % "joda-time" % "2.10.3"

  lazy val jbcrypt = "org.mindrot" % "jbcrypt" % "0.4"

  lazy val kindProjector = "org.typelevel" %% "kind-projector" % "0.10.3"

  lazy val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.1"

  lazy val doobiePostgres =  "org.tpolecat" %% "doobie-postgres" % "0.7.0"

  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"

  lazy val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
}
