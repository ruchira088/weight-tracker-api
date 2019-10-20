package com.ruchij.types

import cats.Applicative
import cats.effect.Sync

import scala.language.higherKinds

object Utils {
  def predicate[F[_]: Sync](condition: Boolean, throwable: => Throwable): F[Unit] =
    if (condition) Sync[F].raiseError(throwable) else Applicative[F].unit
}
