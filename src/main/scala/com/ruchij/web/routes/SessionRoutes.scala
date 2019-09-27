package com.ruchij.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.web.requests.LoginRequest
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object SessionRoutes {
  def apply[F[_]: Sync](authenticationService: AuthenticationService[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of {
      case request @ POST -> Root =>
        for {
          loginRequest <- request.as[LoginRequest]
          authenticationToken <- authenticationService.login(loginRequest.username, loginRequest.password)
          response <- Created(authenticationToken)
        } yield response
    }
  }
}
