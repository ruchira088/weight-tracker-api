package com.ruchij.web

import cats.data.{Kleisli, OptionT, ValidatedNel}
import cats.implicits._
import cats.effect.Sync
import com.ruchij.exceptions._
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.authorization.AuthorizationService
import com.ruchij.services.data.WeightEntryService
import com.ruchij.services.health.HealthCheckService
import com.ruchij.services.user.UserService
import com.ruchij.services.user.models.User
import com.ruchij.types.Transformation
import com.ruchij.web.middleware.authentication.{AuthenticationTokenExtractor, RequestAuthenticator}
import com.ruchij.web.responses.ErrorResponse
import com.ruchij.web.routes.Paths.{`/health`, `/session`, `/user`}
import com.ruchij.web.routes.{HealthRoutes, SessionRoutes, UserRoutes}
import io.circe.JsonObject
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.dsl.impl.EntityResponseGenerator
import org.http4s.server.middleware.CORS
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{HttpApp, HttpRoutes, MessageFailure, Request, Response, Status}

import scala.language.higherKinds

object Routes {
  def apply[F[_]: Sync](
    userService: UserService[F],
    weightEntryService: WeightEntryService[F],
    healthCheckService: HealthCheckService[F]
  )(
    implicit authenticationService: AuthenticationService[F],
    authorizationService: AuthorizationService[F],
    transformation: Transformation[ValidatedNel[Throwable, *], F]
  ): HttpApp[F] = {

    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    implicit val authMiddleware: AuthMiddleware[F, User] =
      RequestAuthenticator.authenticationMiddleware(
        authenticationService,
        AuthenticationTokenExtractor.bearerTokenExtractor
      )

    val router: Kleisli[OptionT[F, *], Request[F], Response[F]] =
      Router(
        `/user` -> UserRoutes(userService, weightEntryService),
        `/session` -> SessionRoutes(authenticationService),
        `/health` -> HealthRoutes(healthCheckService)
      )

    CORS {
      Kleisli {
        request: Request[F] => router.run(request).getOrElse(Response.notFound)
      }
    }
  }

  def responseHandler[F[_]: Sync](httpApp: HttpApp[F]): HttpApp[F] =
    Kleisli[F, Request[F], Response[F]] { request =>
      Sync[F].handleErrorWith(httpApp.run(request)) { throwable =>
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

      case ValidationException(_) => Status.BadRequest

      case _: MessageFailure => Status.BadRequest

      case _ => Status.InternalServerError
    }

  private def throwableErrorResponseMapper(throwable: Throwable): ErrorResponse =
    throwable match {
      case AggregatedException(errors) => ErrorResponse(errors)

      case _ => ErrorResponse(List(throwable))
    }
}
