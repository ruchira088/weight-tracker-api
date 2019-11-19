package com.ruchij.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.services.health.HealthCheckService
import com.ruchij.web.responses.HealthCheckResponse
import com.ruchij.web.middleware.correlation.CorrelationId.`with`
import com.ruchij.web.routes.Paths.services
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object HealthRoutes {
  def apply[F[_]: Sync](healthCheckService: HealthCheckService[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of {
      case GET -> Root `with` correlationId =>
        for {
          serviceInformation <- healthCheckService.serviceInformation()
          response <- Ok(serviceInformation)
        } yield response

      case GET -> Root / `services` `with` correlationId =>
        for {
          databaseHealthStatus <- healthCheckService.database()
          redisHealthStatus <- healthCheckService.redis()

          healthCheckResponse = HealthCheckResponse(databaseHealthStatus, redisHealthStatus)

          response <-
            if (HealthCheckResponse.isAllHealthy(healthCheckResponse))
              Ok(healthCheckResponse)
            else
              ServiceUnavailable(healthCheckResponse)
        }
        yield response
    }
  }
}
