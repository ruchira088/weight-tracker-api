package com.ruchij.messaging

import akka.stream.scaladsl.Source
import com.ruchij.messaging.models.{CommittableMessage, Topic}

import scala.language.higherKinds

trait Subscriber[F[_]] {
  type CommitResult
  type SubscriptionResult

  def subscribe[A](topic: Topic[A]): Source[CommittableMessage[F, A, CommitResult], SubscriptionResult]
}
