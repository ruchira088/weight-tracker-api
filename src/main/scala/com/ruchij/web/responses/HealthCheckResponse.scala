package com.ruchij.web.responses

import cats.Applicative
import com.ruchij.services.health.models.HealthStatus
import io.circe.generic.auto._
import org.http4s.circe.jsonEncoderOf
import org.http4s.EntityEncoder

import scala.language.higherKinds

case class HealthCheckResponse(
  database: HealthStatus,
  redis: HealthStatus,
  publisher: HealthStatus,
  resourceStorage: HealthStatus
)

object HealthCheckResponse {
  implicit def healthCheckResponse[F[_]: Applicative]: EntityEncoder[F, HealthCheckResponse] =
    jsonEncoderOf[F, HealthCheckResponse]

  def isAllHealthy(healthCheckResponse: HealthCheckResponse): Boolean =
    List(
      healthCheckResponse.database,
      healthCheckResponse.redis,
      healthCheckResponse.publisher,
      healthCheckResponse.resourceStorage
    ).forall(_ == HealthStatus.Healthy)
}
