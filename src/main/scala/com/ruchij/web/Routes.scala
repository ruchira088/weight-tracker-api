package com.ruchij.web

import cats.data.Kleisli
import cats.effect.Sync
import com.ruchij.exceptions.{AuthenticationException, ResourceConflictException, ResourceNotFoundException}
import com.ruchij.services.user.models.User
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.authorization.AuthorizationService
import com.ruchij.services.data.WeightEntryService
import com.ruchij.services.health.HealthCheckService
import com.ruchij.services.user.UserService
import com.ruchij.web.middleware.authentication.{AuthenticationTokenExtractor, RequestAuthenticator}
import com.ruchij.web.responses.ErrorResponse
import com.ruchij.web.routes.{HealthRoutes, SessionRoutes, UserRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.EntityResponseGenerator
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{HttpApp, HttpRoutes, Request, Response, Status}

import scala.language.higherKinds

object Routes {
  def apply[F[_]: Sync](
    userService: UserService[F],
    weightEntryService: WeightEntryService[F],
    healthCheckService: HealthCheckService[F]
  )(
    implicit authenticationService: AuthenticationService[F],
    authorizationService: AuthorizationService[F]
  ): HttpRoutes[F] = {

    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    implicit val authMiddleware: AuthMiddleware[F, User] =
      RequestAuthenticator.authenticationMiddleware(
        authenticationService,
        AuthenticationTokenExtractor.bearerTokenExtractor
      )

    Router(
      "/user" -> UserRoutes(userService, weightEntryService),
      "/session" -> SessionRoutes(authenticationService),
      "/health" -> HealthRoutes(healthCheckService)
    )
  }

  def responseHandler[F[_]: Sync](routes: HttpRoutes[F]): HttpApp[F] =
    Kleisli[F, Request[F], Response[F]] { request =>
      Sync[F].handleErrorWith(routes.run(request).getOrElse(Response.notFound)) { throwable =>
        val entityResponseGenerator =
          new EntityResponseGenerator[F, F] {
            override def status: Status = throwableStatusMapper(throwable)
          }

        entityResponseGenerator(throwableErrorResponseMapper(throwable))
      }
    }

  private def throwableStatusMapper(throwable: Throwable): Status =
    throwable match {
      case _: ResourceConflictException => Status.Conflict

      case _: ResourceNotFoundException => Status.NotFound

      case _: AuthenticationException => Status.Unauthorized
    }

  private def throwableErrorResponseMapper(throwable: Throwable): ErrorResponse =
    throwable match {
      case _ => ErrorResponse(throwable)
    }
}
