package com.ruchij.web.responses

import cats.Applicative
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.web.circe.Encoders.jodaTimeEncoder
import org.http4s.EntityEncoder
import org.joda.time.DateTime
import io.circe.generic.auto._
import org.http4s.circe.jsonEncoderOf

import scala.language.higherKinds
import scala.util.Properties

case class HealthCheck(
  serviceName: String,
  serviceVersion: String,
  javaVersion: String,
  sbtVersion: String,
  scalaVersion: String,
  currentTimestamp: DateTime
)

object HealthCheck {
  def apply(dateTime: DateTime): HealthCheck =
    HealthCheck(
      BuildInfo.name,
      BuildInfo.version,
      Properties.javaVersion,
      BuildInfo.sbtVersion,
      BuildInfo.scalaVersion,
      dateTime
    )

  implicit def jsonEncoder[F[_]: Applicative]: EntityEncoder[F, HealthCheck] = jsonEncoderOf[F, HealthCheck]
}
