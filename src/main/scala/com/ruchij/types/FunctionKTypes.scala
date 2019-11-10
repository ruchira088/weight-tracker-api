package com.ruchij.types

import cats.data.ValidatedNel
import cats.effect.{ContextShift, IO}
import cats.~>
import com.ruchij.exceptions.AggregatedException

import scala.concurrent.Future

object FunctionKTypes {
  implicit def futureToIO(implicit contextShift: ContextShift[IO]): Future ~> IO =
    new ~>[Future, IO] {
      override def apply[A](future: Future[A]): IO[A] = IO.fromFuture(IO(future))
    }

  implicit def validateNelToIO: ValidatedNel[Throwable, *] ~> IO =
    new ~>[ValidatedNel[Throwable, *], IO] {
      override def apply[A](validatedNel: ValidatedNel[Throwable, A]): IO[A] =
        validatedNel.fold[IO[A]](
          errors => IO.raiseError(AggregatedException(errors.toList)),
          value => IO.pure(value)
        )
    }

  implicit def eitherThrowableToIO: Either[Throwable, *] ~> IO =
    new ~>[Either[Throwable, *], IO] {
      override def apply[A](either: Either[Throwable, A]): IO[A] = IO.fromEither(either)
    }
}
