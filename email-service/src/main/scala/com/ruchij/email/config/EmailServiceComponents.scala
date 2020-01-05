package com.ruchij.email.config

import java.nio.file.Paths
import java.util.concurrent.Executors

import akka.actor.ActorSystem
import cats.effect.{Async, ContextShift, Sync}
import cats.implicits._
import cats.{Applicative, ~>}
import com.ruchij.config.KafkaClientConfiguration
import com.ruchij.config.development.ApplicationMode
import com.ruchij.config.development.ApplicationMode.{DockerCompose, Production}
import com.ruchij.email.{ConsoleEmailService, EmailService, SendGridEmailService}
import com.ruchij.messaging.Subscriber
import com.ruchij.messaging.file.FileBasedSubscriber
import com.ruchij.messaging.kafka.KafkaSubscriber
import com.sendgrid.SendGrid
import pureconfig.{ConfigObjectSource, ConfigReader}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

case class EmailServiceComponents[F[_]](emailService: EmailService[F], subscriber: Subscriber[F])

object EmailServiceComponents {
  def from[F[_]: Async: ContextShift: *[_] ~> Future: Future ~> *[_]: Either[Throwable, *] ~> *[_], G[_]: Sync: ConfigReader.Result ~> *[_]](
    applicationMode: ApplicationMode,
    configObjectSource: ConfigObjectSource
  )(implicit actorSystem: ActorSystem): G[EmailServiceComponents[F]] =
    applicationMode match {
      case Production =>
        for {
          kafkaClientConfiguration <- KafkaClientConfiguration.confluent[G](configObjectSource)
          kafkaSubscriber = new KafkaSubscriber[F](kafkaClientConfiguration)

          sendgridConfiguration <- SendgridConfiguration.load[G](configObjectSource)
          ioBlockingExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
          sendgridEmailService = new SendGridEmailService[F](
            new SendGrid(sendgridConfiguration.apiKey),
            ioBlockingExecutionContext
          )
        } yield EmailServiceComponents(sendgridEmailService, kafkaSubscriber)

      case DockerCompose =>
        for {
          kafkaClientConfiguration <- KafkaClientConfiguration.local[G](configObjectSource)
          kafkaSubscriber = new KafkaSubscriber[F](kafkaClientConfiguration)
        }
        yield EmailServiceComponents(new ConsoleEmailService[F], kafkaSubscriber)

      case _ =>
        Applicative[G].pure {
          EmailServiceComponents(
            new ConsoleEmailService[F],
            new FileBasedSubscriber[F](Paths.get("./file-based-messaging.txt"))
          )
        }
    }
}
