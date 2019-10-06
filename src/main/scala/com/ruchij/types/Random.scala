package com.ruchij.types

import java.util.UUID

import cats.effect.Sync

import scala.language.higherKinds

trait Random[F[_], +A] {
  def value[B >: A]: F[B]
}

object Random {
  def apply[F[_], A](implicit random: Random[F, A]): Random[F, A] = random

  implicit def randomUuid[F[_]: Sync]: Random[F, UUID] = new Random[F, UUID] {
    override def value[B >: UUID]: F[B] = Sync[F].delay(UUID.randomUUID())
  }
}
