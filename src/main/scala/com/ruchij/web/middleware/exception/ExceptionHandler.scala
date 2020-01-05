package com.ruchij.web.middleware.exception

import cats.arrow.FunctionK
import cats.data.Kleisli
import cats.effect.Sync
import com.ruchij.exceptions._
import com.ruchij.web.responses.ErrorResponse
import org.http4s.dsl.impl.EntityResponseGenerator
import org.http4s.{HttpApp, Request, Response, Status}

import scala.language.higherKinds

object ExceptionHandler {

  def apply[F[_]: Sync](httpApp: HttpApp[F]): HttpApp[F] =
    Kleisli[F, Request[F], Response[F]] { request =>
      Sync[F].handleErrorWith(httpApp.run(request)) { throwable =>
        entityResponseGenerator(throwable)(throwableErrorResponseMapper(throwable))
      }
    }

  private def entityResponseGenerator[F[_]](throwable: Throwable): EntityResponseGenerator[F, F] =
    new EntityResponseGenerator[F, F] {
      override def status: Status =
        throwable match {
          case ValidationException(_) => Status.BadRequest

          case _: ResourceConflictException => Status.Conflict

          case _: ResourceNotFoundException => Status.NotFound

          case _: AuthenticationException => Status.Unauthorized

          case _: AuthorizationException => Status.Forbidden

          case _: LockedUserAccountException => Status.Unauthorized

          case _ => Status.InternalServerError
        }

      override def liftG: FunctionK[F, F] = FunctionK.id[F]
    }

  private def throwableErrorResponseMapper(throwable: Throwable): ErrorResponse =
    throwable match {
      case AggregatedException(errors) => ErrorResponse(errors)

      case _ => ErrorResponse(List(throwable))
    }
}
