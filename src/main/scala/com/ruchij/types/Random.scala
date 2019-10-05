package com.ruchij.types

import java.util.UUID

import cats.effect.IO

import scala.language.higherKinds

trait Random[F[_], +A] {
  def value[B >: A]: F[B]
}

object Random {
  def apply[F[_], A](implicit random: Random[F, A]): Random[F, A] = random

  implicit val ioRandomUuid: Random[IO, UUID] = new Random[IO, UUID] {
    override def value[B >: UUID]: IO[B] = IO.delay(UUID.randomUUID())
  }
}
