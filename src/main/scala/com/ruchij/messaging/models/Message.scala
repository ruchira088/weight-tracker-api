package com.ruchij.messaging.models

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

case class Message[A](topic: Topic[A], value: A)

object Message {
  def apply[A](value: A)(implicit topic: Topic[A]): Message[A] = Message(topic, value)

  implicit def messageEncoder[A: Encoder]: Encoder[Message[A]] = deriveEncoder[Message[A]]
}
