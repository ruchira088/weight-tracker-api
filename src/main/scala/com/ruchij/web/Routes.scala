package com.ruchij.web

import cats.data.{Kleisli, OptionT, ValidatedNel}
import cats.effect.Sync
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.authorization.AuthorizationService
import com.ruchij.services.data.WeightEntryService
import com.ruchij.services.health.HealthCheckService
import com.ruchij.services.user.UserService
import com.ruchij.services.user.models.User
import com.ruchij.types.Transformation
import com.ruchij.types.Transformation.~>
import com.ruchij.web.middleware.authentication.{AuthenticationTokenExtractor, RequestAuthenticator}
import com.ruchij.web.middleware.exception.ExceptionHandler
import com.ruchij.web.routes.Paths.{`/health`, `/session`, `/user`}
import com.ruchij.web.routes.{HealthRoutes, SessionRoutes, UserRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.CORS
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{HttpApp, Request, Response}

import scala.language.higherKinds

object Routes {

  def apply[F[_]: Sync: Lambda[X[_] => ValidatedNel[Throwable, *] ~> X]](
    userService: UserService[F],
    weightEntryService: WeightEntryService[F],
    healthCheckService: HealthCheckService[F],
    authenticationService: AuthenticationService[F],
    authorizationService: AuthorizationService[F]
  ): HttpApp[F] = {

    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    implicit val authMiddleware: AuthMiddleware[F, User] =
      RequestAuthenticator.authenticationMiddleware(
        authenticationService,
        AuthenticationTokenExtractor.bearerTokenExtractor
      )

    val router: Kleisli[OptionT[F, *], Request[F], Response[F]] =
      Router(
        `/user` -> UserRoutes(userService, weightEntryService, authorizationService),
        `/session` -> SessionRoutes(authenticationService),
        `/health` -> HealthRoutes(healthCheckService)
      )

    ExceptionHandler {
      CORS {
        Kleisli { request: Request[F] =>
          router.run(request).getOrElse(Response.notFound)
        }
      }
    }
  }
}
