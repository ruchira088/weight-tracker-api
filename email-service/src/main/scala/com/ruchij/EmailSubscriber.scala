package com.ruchij

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import cats.effect.Sync
import cats.implicits._
import com.ruchij.email.EmailService
import com.ruchij.email.models.Email
import com.ruchij.messaging.Subscriber
import com.ruchij.messaging.models.{CommittableMessage, Message, Topic}
import com.ruchij.services.user.models.User
import com.ruchij.types.KFunctionK
import com.ruchij.types.KFunctionK.<~>

import scala.concurrent.Future
import scala.language.higherKinds

object EmailSubscriber {
  def run[F[_]: Sync: *[_] <~> Future, A](subscriber: Subscriber[F], topic: Topic[A], onValue: A => F[_])(implicit materializer: Materializer): F[Done] =
    Sync[F].defer {
      KFunctionK[F, Future].from {
        subscriber
          .subscribe(topic)
          .mapAsync(parallelism = 1) {
            case CommittableMessage(Message(_, value), commit) =>
              KFunctionK[F, Future].to(onValue(value).product(commit))
          }
          .runWith(Sink.ignore)
      }
    }

  def run[F[_]: Sync: *[_] <~> Future](subscriber: Subscriber[F], emailService: EmailService[F])(implicit materializer: Materializer): F[Done] =
    List(run[F, User](subscriber, Topic.UserCreated, user => emailService.send(Email.welcomeEmail(user))))
      .traverse(identity)
      .as(Done)
}
