package com.ruchij.services.health.models

import cats.Applicative
import com.eed3si9n.ruchij.BuildInfo
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import org.joda.time.DateTime
import com.ruchij.web.circe.Encoders.jodaTimeEncoder

import scala.language.higherKinds
import scala.util.Properties

case class ServiceInformation(
  serviceName: String,
  serviceVersion: String,
  javaVersion: String,
  sbtVersion: String,
  scalaVersion: String,
  currentTimestamp: DateTime
)

object ServiceInformation {
  def apply(dateTime: DateTime): ServiceInformation =
    ServiceInformation(
      BuildInfo.name,
      BuildInfo.version,
      Properties.javaVersion,
      BuildInfo.sbtVersion,
      BuildInfo.scalaVersion,
      dateTime
    )

  implicit def jsonEncoder[F[_]: Applicative]: EntityEncoder[F, ServiceInformation] = jsonEncoderOf[F, ServiceInformation]
}
