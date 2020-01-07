package com.ruchij.web

import java.util.UUID

import cats.data.ValidatedNel
import cats.effect.{ContextShift, Sync}
import cats.~>
import cats.implicits._
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.authorization.AuthorizationService
import com.ruchij.services.data.WeightEntryService
import com.ruchij.services.health.HealthCheckService
import com.ruchij.services.user.UserService
import com.ruchij.services.user.models.User
import com.ruchij.types.Random
import com.ruchij.web.assets.ResourceFileService
import com.ruchij.web.middleware.authentication.{AuthenticationTokenExtractor, RequestAuthenticator}
import com.ruchij.web.middleware.correlation.CorrelationId
import com.ruchij.web.middleware.exception.ExceptionHandler
import com.ruchij.web.middleware.notfound.NotFoundHandler
import com.ruchij.web.routes.Paths.{`/health`, `/session`, `/user`, `/v1`}
import com.ruchij.web.routes.{HealthRoutes, SessionRoutes, StaticAssetRoutes, UserRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.CORS
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{HttpApp, HttpRoutes}

import scala.language.higherKinds

object Routes {

  def apply[F[_]: Sync: ValidatedNel[Throwable, *] ~> *[_]: Random[*[_], UUID]: ContextShift](
    userService: UserService[F],
    weightEntryService: WeightEntryService[F],
    healthCheckService: HealthCheckService[F],
    authenticationService: AuthenticationService[F],
    authorizationService: AuthorizationService[F],
    resourceFileService: ResourceFileService[F],
  ): HttpApp[F] = {

    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    val authenticationTokenExtractor = AuthenticationTokenExtractor.bearerTokenExtractor[F]

    implicit val authMiddleware: AuthMiddleware[F, User] =
      RequestAuthenticator.authenticationMiddleware(authenticationService, authenticationTokenExtractor)

    val router: HttpRoutes[F] =
      StaticAssetRoutes(resourceFileService) <+>
        Router(
          `/v1` ->
            Router(
              `/user` -> UserRoutes(userService, weightEntryService, authorizationService),
              `/session` -> SessionRoutes(authenticationService, authenticationTokenExtractor),
            ),
          `/health` -> HealthRoutes(healthCheckService)
        )

    CORS {
      CorrelationId.inject {
        ExceptionHandler {
          NotFoundHandler(router)
        }
      }
    }
  }
}
