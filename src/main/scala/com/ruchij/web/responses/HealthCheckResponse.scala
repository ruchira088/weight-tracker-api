package com.ruchij.web.responses

import cats.Applicative
import com.ruchij.circe.Encoders.enumEncoder
import com.ruchij.services.health.models.HealthStatus
import io.circe.generic.auto._
import org.http4s.circe.jsonEncoderOf
import org.http4s.EntityEncoder

import scala.language.higherKinds

case class HealthCheckResponse(database: HealthStatus, redis: HealthStatus)

object HealthCheckResponse {
  implicit def healthCheckResponse[F[_]: Applicative]: EntityEncoder[F, HealthCheckResponse] =
    jsonEncoderOf[F, HealthCheckResponse]

  def isAllHealthy(healthCheckResponse: HealthCheckResponse): Boolean =
    List(healthCheckResponse.database, healthCheckResponse.redis).forall(_ == HealthStatus.Healthy)
}
