package com.ruchij.daos

import cats.Applicative
import cats.effect.Bracket
import cats.implicits._
import com.ruchij.exceptions.ResourceConflictException

import scala.language.higherKinds

package object doobie {
  def singleUpdate[F[_]: Bracket[*[_], Throwable]](result: F[Int]): F[Boolean] =
    result.flatMap {
      case 0 => Applicative[F].pure(false)
      case 1 => Applicative[F].pure(true)
      case _ => Bracket[F, Throwable].raiseError(ResourceConflictException("Multiple records updated"))
    }
}
