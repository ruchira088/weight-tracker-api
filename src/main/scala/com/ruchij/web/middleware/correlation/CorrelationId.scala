package com.ruchij.web.middleware.correlation

import java.util.UUID

import cats.Applicative
import cats.data.Kleisli
import cats.effect.Sync
import cats.implicits._
import com.ruchij.types.Random
import com.ruchij.web.headers.`X-Correlation-ID`
import com.ruchij.web.headers.`X-Correlation-ID`.CorrelationId
import org.http4s.{HttpApp, Request, Response}

import scala.language.higherKinds

object CorrelationId {
  object withId {
    def unapply[F[_]](request: Request[F]): Option[(Request[F], CorrelationId)] =
      request.headers.get(`X-Correlation-ID`)
        .map {
          header => (request, header.correlationId)
        }
  }

  def inject[F[_]: Sync: Random[*[_], UUID]](httpApp: HttpApp[F]): HttpApp[F] =
    Kleisli[F, Request[F], Response[F]] { request =>
      request.headers.get(`X-Correlation-ID`)
        .map(_.value)
        .fold(Random[F, UUID].value.map(_.toString))(correlationId => Applicative[F].pure(correlationId))
        .flatMap {
          correlationId =>
            httpApp.run(request.putHeaders(`X-Correlation-ID`.from(correlationId)))
              .map(_.putHeaders(`X-Correlation-ID`.from(correlationId)))
        }
    }
}
