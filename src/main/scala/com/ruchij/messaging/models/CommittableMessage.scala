package com.ruchij.messaging.models

import com.ruchij.messaging.Subscriber

import scala.language.higherKinds

case class CommittableMessage[F[_], A, B <: Subscriber[F]#CommitResult](message: Message[A], commit: F[B])
