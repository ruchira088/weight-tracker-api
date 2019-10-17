package com.ruchij.web.routes

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.implicits._
import com.ruchij.exceptions.ResourceNotFoundException
import com.ruchij.web.routes.Paths.user
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.user.models.User
import com.ruchij.web.middleware.authentication.AuthenticationTokenExtractor
import com.ruchij.web.requests.bodies.LoginRequest
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes, Request, Response}

import scala.language.higherKinds

object SessionRoutes {
  def apply[F[_]: Sync](authenticationService: AuthenticationService[F], authenticationTokenExtractor: AuthenticationTokenExtractor[F])(implicit dsl: Http4sDsl[F], authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = {
    import dsl._

    val publicRoutes: HttpRoutes[F] =
      HttpRoutes.of {
        case request @ POST -> Root =>
          for {
            loginRequest <- request.as[LoginRequest]
            authenticationToken <- authenticationService.login(loginRequest.email, loginRequest.password)
            response <- Created(authenticationToken)
          } yield response
      }

    val authenticatedRoutes: Kleisli[OptionT[F, *], Request[F], Response[F]] =
      authMiddleware {
        AuthedRoutes.of {
          case GET -> Root / `user` as authenticatedUser => Ok(authenticatedUser)

          case authenticatedRequest @ DELETE -> Root as _ =>
            for {
              secret <-
                authenticationTokenExtractor.extract(authenticatedRequest.req)
                  .getOrElseF(Sync[F].raiseError(ResourceNotFoundException("Unable to find authentication token")))

              authenticationToken <- authenticationService.logout(secret)

              response <- Ok(authenticationToken)
            }
            yield response
        }
      }

    publicRoutes <+> authenticatedRoutes
  }
}
