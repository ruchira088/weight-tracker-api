package com.ruchij.web.responses

import cats.Applicative
import com.ruchij.services.health.models.HealthStatus
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import shapeless.Generic

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

  val healthCheckResponseGeneric = Generic[HealthCheckResponse]

  def isAllHealthy(healthCheckResponse: HealthCheckResponse): Boolean =
    healthCheckResponseGeneric.to(healthCheckResponse).toList.forall(_ == HealthStatus.Healthy)
}
