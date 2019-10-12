package com.ruchij.types

import cats.data.ValidatedNel
import cats.effect.{ContextShift, IO}
import com.ruchij.exceptions.AggregatedException

import scala.concurrent.Future
import scala.language.higherKinds

/**
 * This type class differs with [[cats.arrow.FunctionK]] ([[cats.~>]]) by the argument to the apply function being
 * call-by-name rather then call-by-value.
 */
trait Transformation[F[_], G[_]] {
  def apply[A](value: => F[A]): G[A]
}

object Transformation {
  type ~>[F[_], G[_]] = Transformation[F, G]

  def apply[F[_], G[_]](implicit transformation: F ~> G): F ~> G = transformation

  implicit def futureToIo(implicit contextShift: ContextShift[IO]): Future ~> IO =
    new Transformation[Future, IO] {
      override def apply[A](value: => Future[A]): IO[A] = IO.fromFuture(IO(value))
    }

  implicit def validatedNelToIo[Err <: Throwable]: ValidatedNel[Err, *] ~> IO =
    new Transformation[ValidatedNel[Err, *], IO] {
      override def apply[A](value: => ValidatedNel[Err, A]): IO[A] =
        value.fold[IO[A]](errors => IO.raiseError(AggregatedException(errors.toList)), result => IO(result))
    }

  implicit def eitherThrowableToIo[Err <: Throwable]: Either[Err, *] ~> IO =
    new Transformation[Either[Err, *], IO] {
      override def apply[A](value: => Either[Err, A]): IO[A] =
        value.fold(IO.raiseError, IO.pure)
    }
}
