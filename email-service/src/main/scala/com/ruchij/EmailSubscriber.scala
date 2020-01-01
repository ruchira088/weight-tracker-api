package com.ruchij

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.ruchij.email.EmailService
import com.ruchij.messaging.Subscriber
import com.ruchij.messaging.models.Topic

import scala.language.higherKinds

object EmailSubscriber {
  def run[F[_]](subscriber: Subscriber[F], emailService: EmailService[F])(implicit materializer: Materializer) =
    subscriber.subscribe(Topic.UserCreated)
      .map {
        case (user, value) => println(user)
      }
      .runWith(Sink.ignore)
}
