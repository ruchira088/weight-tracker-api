package com.ruchij.web.middleware.notfound

import cats.MonadError
import cats.data.Kleisli
import com.ruchij.exceptions.ResourceNotFoundException
import org.http4s.{HttpApp, HttpRoutes, Request, Response}

import scala.language.higherKinds

object NotFoundHandler {
  def apply[F[_]: MonadError[*[_], Throwable]](httpRoutes: HttpRoutes[F]): HttpApp[F] =
    Kleisli[F, Request[F], Response[F]] {
      request =>
        httpRoutes.run(request).getOrElseF {
          MonadError[F, Throwable].raiseError {
            ResourceNotFoundException(s"Endpoint not found: ${request.method} ${request.uri}")
          }
        }
    }
}
