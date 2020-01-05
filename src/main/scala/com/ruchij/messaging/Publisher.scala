package com.ruchij.messaging

import com.ruchij.messaging.models.Message

import scala.language.higherKinds

trait Publisher[F[_], Acknowledgement] {
  def publish[A](message: Message[A]): F[Acknowledgement]
}
