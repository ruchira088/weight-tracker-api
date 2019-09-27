package com.ruchij.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.services.health.HealthCheckService
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object HealthRoutes {
  def apply[F[_]: Sync](healthCheckService: HealthCheckService[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of {
      case GET -> Root =>
        for {
          serviceInformation <- healthCheckService.serviceInformation()
          response <- Ok(serviceInformation)
        } yield response
    }
  }
}
