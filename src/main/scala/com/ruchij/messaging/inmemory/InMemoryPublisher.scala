package com.ruchij.messaging.inmemory

import cats.effect.Sync
import com.ruchij.messaging.Publisher
import com.ruchij.messaging.models.Message

import scala.collection.mutable
import scala.language.higherKinds

class InMemoryPublisher[F[_]: Sync](val queue: mutable.Queue[Message[_]]) extends Publisher[F, Unit] {
  override def publish[A](message: Message[A]): F[Unit] =
    Sync[F].delay(queue += message)
}

object InMemoryPublisher {
  def empty[F[_]: Sync]: InMemoryPublisher[F] = new InMemoryPublisher(mutable.Queue.empty[Message[_]])
}
