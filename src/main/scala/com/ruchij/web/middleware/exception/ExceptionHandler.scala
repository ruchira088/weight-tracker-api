package com.ruchij.web.middleware.exception

import cats.data.Kleisli
import cats.effect.Sync
import com.ruchij.exceptions._
import com.ruchij.web.responses.ErrorResponse
import org.http4s.dsl.impl.EntityResponseGenerator
import org.http4s.{HttpApp, MessageFailure, Request, Response, Status}

import scala.language.higherKinds

object ExceptionHandler {

  def apply[F[_]: Sync](httpApp: HttpApp[F]): HttpApp[F] =
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
