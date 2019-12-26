package com.ruchij.services.health.models

import cats.Applicative
import com.eed3si9n.ruchij.BuildInfo
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import org.joda.time.DateTime
import com.ruchij.circe.Encoders.{enumEncoder, jodaTimeEncoder}
import com.ruchij.config.BuildInformation
import com.ruchij.config.development.ApplicationMode

import scala.language.higherKinds
import scala.util.Properties

case class ServiceInformation(
  applicationMode: ApplicationMode,
  serviceName: String,
  serviceVersion: String,
  organization: String,
  javaVersion: String,
  sbtVersion: String,
  scalaVersion: String,
  currentTimestamp: DateTime,
  gitBranch: Option[String],
  gitCommit: Option[String],
  buildTimestamp: Option[DateTime]
)

object ServiceInformation {
  def apply(applicationMode: ApplicationMode, dateTime: DateTime, buildInformation: BuildInformation): ServiceInformation =
    ServiceInformation(
      applicationMode,
      BuildInfo.name,
      BuildInfo.version,
      BuildInfo.organization,
      Properties.javaVersion,
      BuildInfo.sbtVersion,
      BuildInfo.scalaVersion,
      dateTime,
      buildInformation.gitBranch,
      buildInformation.gitCommit,
      buildInformation.buildTimestamp
    )

  implicit def jsonEncoder[F[_]: Applicative]: EntityEncoder[F, ServiceInformation] = jsonEncoderOf[F, ServiceInformation]
}
