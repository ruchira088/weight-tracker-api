package com.ruchij.messaging.file

import java.nio.file.Path
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import cats.effect.{Async, Sync}
import cats.implicits._
import cats.{Applicative, ~>}
import com.ruchij.messaging.Subscriber
import com.ruchij.messaging.file.FileBasedSubscriber.SimpleMessage
import com.ruchij.messaging.models.{CommittableMessage, Message, Topic}
import com.ruchij.utils.FileUtils
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.parse
import io.circe.{Decoder, Json}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.{higherKinds, postfixOps}

class FileBasedSubscriber[F[_]: Async](filePath: Path)(implicit functionK: Either[Throwable, *] ~> F, toFuture: F ~> Future) extends Subscriber[F] {
  override type CommitResult = Int
  override type SubscriptionResult = Cancellable

  override def subscribe[A](topic: Topic[A]): Source[CommittableMessage[F, A, CommitResult], Cancellable] = {
    val currentIndex = new AtomicInteger(0)
    val pendingCommit = new AtomicBoolean(false)

    Source.tick[Unit](0 seconds, 500 milliseconds, (): Unit)
      .mapAsync(parallelism = 1) {
        _ =>
         toFuture {
           for {
             contents <- FileUtils.readFile(filePath)
             jsonList <- new String(contents).split("\n").toList.traverse(line => functionK(parse(line)))
             simpleMessages <- jsonList.traverse(json => functionK(json.as[SimpleMessage]))
             message <-
               simpleMessages
                 .filter(_.topic.equalsIgnoreCase(topic.entryName))
                 .drop(currentIndex.get())
                 .headOption
                 .map(simpleMessage => functionK(topic.codec.decodeJson(simpleMessage.value)))
                 .fold[F[Option[A]]](Applicative[F].pure(None))(_.map(Some.apply))

             send <-
               message.filter(_ => !pendingCommit.get()).map {
                 value =>
                   Sync[F].delay {
                     pendingCommit.set(true)

                     CommittableMessage[F, A, CommitResult](
                       Message(topic, value),
                       Sync[F].delay {
                         val index = currentIndex.incrementAndGet()
                         pendingCommit.set(false)
                         index
                       }
                     )
                   }
               }
                 .toList
                 .traverse(identity)
           }
             yield send
         }
      }
      .mapConcat(identity[List[CommittableMessage[F, A, CommitResult]]])
  }
}

object FileBasedSubscriber {
  case class SimpleMessage(topic: String, value: Json)

  implicit def messageTopicDecoder: Decoder[SimpleMessage] = deriveDecoder
}
