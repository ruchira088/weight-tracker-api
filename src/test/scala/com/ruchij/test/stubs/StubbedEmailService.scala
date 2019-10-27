package com.ruchij.test.stubs

import java.util.concurrent.ConcurrentLinkedQueue

import cats.Applicative
import com.ruchij.services.email.EmailService
import com.ruchij.services.email.models.Email

import scala.language.higherKinds

class StubbedEmailService[F[_]: Applicative](concurrentLinkedQueue: ConcurrentLinkedQueue[Email])
    extends EmailService[F] {
  override type Response = Boolean

  override def send(email: Email): F[Response] =
    Applicative[F].point(concurrentLinkedQueue.add(email))
}
