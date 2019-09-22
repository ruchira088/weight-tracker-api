package com.ruchij.web

import cats.data.Kleisli
import cats.effect.Sync
import cats.implicits._
import com.ruchij.exceptions.{AuthenticationException, ResourceConflictException, ResourceNotFoundException}
import com.ruchij.services.health.HealthCheckService
import com.ruchij.services.user.UserService
import com.ruchij.circe.EntityEncoders.userEncoder
import com.ruchij.web.requests.CreateUserRequest
import com.ruchij.web.responses.ErrorResponse
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.EntityResponseGenerator
import org.http4s.{HttpApp, HttpRoutes, Request, Response, Status}

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
        } yield response

      case request @ POST -> Root / "user" =>
        for {
          CreateUserRequest(username, password, email, firstName, lastName) <- request.as[CreateUserRequest]
          user <- userService.create(username, password, email, firstName, lastName)
          response <- Created(user)
        } yield response
    }
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
