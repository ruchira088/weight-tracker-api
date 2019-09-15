package com.ruchij.web

import cats.effect.Sync
import cats.implicits._
import com.ruchij.services.health.HealthCheckService
import com.ruchij.services.user.UserService
import com.ruchij.web.circe.EntityEncoders.userEncoder
import com.ruchij.web.request.CreateUserRequest
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object Routes {
  def apply[F[_]: Sync](userService: UserService[F], healthCheckService: HealthCheckService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of {
      case GET -> Root / "health" =>
        for {
          serviceInformation <- healthCheckService.serviceInformation()
          response <- Ok(serviceInformation)
        }
        yield response

      case request @ POST -> Root / "user" =>
        for {
          CreateUserRequest(username, password, email, firstName, lastName) <- request.as[CreateUserRequest]
          user <- userService.create(username, password, email, firstName, lastName)
          response <- Created(user)
        }
        yield response
    }
  }
}
