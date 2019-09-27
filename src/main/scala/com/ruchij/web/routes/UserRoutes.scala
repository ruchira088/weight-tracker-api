package com.ruchij.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.models.User
import com.ruchij.services.user.UserService
import com.ruchij.web.requests.CreateUserRequest
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware

import scala.language.higherKinds

object UserRoutes {
  def apply[F[_]: Sync](userService: UserService[F])(
    implicit dsl: Http4sDsl[F], authMiddleware: AuthMiddleware[F, User]
  ): HttpRoutes[F] = {
    import dsl._

    val publicRoutes: HttpRoutes[F] = HttpRoutes.of {
      case request @ POST -> Root =>
        for {
          CreateUserRequest(username, password, email, firstName, lastName) <- request.as[CreateUserRequest]
          user <- userService.create(username, password, email, firstName, lastName)
          response <- Created(user)
        } yield response
    }

    val authenticatedRoutes =
      authMiddleware {
        AuthedRoutes.of {
          case GET -> Root as user => Ok(user)
        }
      }

    publicRoutes <+> authenticatedRoutes
  }
}
