package com.ruchij.messaging.models

case class Message[A](topic: Topic[A], value: A)

object Message {
  def apply[A](value: A)(implicit topic: Topic[A]): Message[A] = Message(topic, value)
}
