package com.ruchij.messaging

import akka.stream.scaladsl.Source
import com.ruchij.messaging.models.Topic

import scala.language.higherKinds

trait Subscriber[F[_]] {
  type CommitResult

  def subscribe[A](topic: Topic[A]): Source[(A, F[CommitResult]), _]
}
