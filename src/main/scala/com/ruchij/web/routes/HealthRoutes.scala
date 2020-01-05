package com.ruchij.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.exceptions.InternalServiceException
import com.ruchij.logging.Logger
import com.ruchij.services.health.HealthCheckService
import com.ruchij.web.responses.HealthCheckResponse
import com.ruchij.web.middleware.correlation.CorrelationId.withId
import com.ruchij.web.routes.Paths.`services`
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object HealthRoutes {
  private val logger = Logger[HttpRoutes.type]

  def apply[F[_]: Sync](healthCheckService: HealthCheckService[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of {
      case GET -> Root withId correlationId =>
        for {
          _ <- logger.infoF[F]("Health check request received")(correlationId)

          serviceInformation <- healthCheckService.serviceInformation()

          _ <- logger.infoF[F]("Health check is OK")(correlationId)

          response <- Ok(serviceInformation)
        } yield response

      case GET -> Root / `services` withId correlationId =>
        for {
          _ <- logger.infoF[F]("External services health check request received")(correlationId)

          healthCheckResponse <- healthCheckService.healthCheck()

          response <- if (HealthCheckResponse.isAllHealthy(healthCheckResponse))
            Ok(healthCheckResponse) <* logger.infoF[F]("External services health check is OK")(correlationId)
          else
            ServiceUnavailable(healthCheckResponse) <*
              logger.errorF[F](InternalServiceException("Services are UNHEALTHY"))(correlationId)

        } yield response
    }
  }
}
