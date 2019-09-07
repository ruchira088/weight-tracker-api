import sbt._

object Dependencies
{
  val SCALA_VERSION = "2.12.9"
  val HTTP4S_VERSION = "0.20.10"

  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % HTTP4S_VERSION

  lazy val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % HTTP4S_VERSION

  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % HTTP4S_VERSION

  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.11.1"

  lazy val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.11.1"

  lazy val jodaTime = "joda-time" % "joda-time" % "2.10.3"

  lazy val kindProjector = "org.typelevel" %% "kind-projector" % "0.10.3"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"

  lazy val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
}
