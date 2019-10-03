package com.ruchij.web.requests

import cats.MonadError
import cats.implicits.toFlatMapOps
import com.ruchij.exceptions.ValidationException
import com.ruchij.web.requests.validators.Validator
import org.http4s.{EntityDecoder, Request}

import scala.language.higherKinds

object RequestParser {
  implicit class Parser[F[_]](val request: Request[F]) extends AnyVal {
    def to[A](
      implicit monadError: MonadError[F, Throwable],
      entityDecoder: EntityDecoder[F, A],
      validator: Validator[F, A]
    ): F[A] =
      request.attemptAs[A]
        .foldF[A](
          failure => monadError.raiseError(ValidationException(failure.getMessage())),
          monadError.pure
        )
        .flatMap(validator.validate)
  }
}
