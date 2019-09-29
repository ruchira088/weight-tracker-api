package com.ruchij.types

import cats.data.ValidatedNel
import cats.effect.{ContextShift, IO}
import com.ruchij.exceptions.AggregatedException

import scala.concurrent.Future
import scala.language.higherKinds

trait Transformation[F[_], G[_]] {
  def apply[A](value: => F[A]): G[A]
}

object Transformation {
  def apply[F[_], G[_]](implicit transformation: F ~> G): F ~> G = transformation

  type ~>[F[_], G[_]] = Transformation[F, G]

  implicit def futureToIo(implicit contextShift: ContextShift[IO]): Future ~> IO =
    new Transformation[Future, IO] {
      override def apply[A](value: => Future[A]): IO[A] = IO.fromFuture(IO(value))
    }

  implicit def validatedNelToIo[Error <: Throwable]: Transformation[ValidatedNel[Error, *], IO] =
    new Transformation[ValidatedNel[Error, *], IO] {
      override def apply[A](value: => ValidatedNel[Error, A]): IO[A] =
        value.fold[IO[A]](errors => IO.raiseError(AggregatedException(errors)), result => IO(result))
    }
}
