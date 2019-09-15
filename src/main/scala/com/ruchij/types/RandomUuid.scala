package com.ruchij.types

import java.util.UUID

import cats.effect.IO

import scala.language.higherKinds

trait RandomUuid[F[_]] {
  val uuid: F[UUID]
}

object RandomUuid {
  def apply[F[_]](implicit randomUuid: RandomUuid[F]): RandomUuid[F] = randomUuid

  implicit val ioRandomUuid: RandomUuid[IO] = new RandomUuid[IO] {
    override val uuid: IO[UUID] = IO.delay(UUID.randomUUID())
  }
}
