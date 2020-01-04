package com.ruchij.email.config

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import cats.effect.{ContextShift, Sync}
import cats.implicits._
import cats.{Applicative, ~>}
import com.ruchij.config.KafkaClientConfiguration
import com.ruchij.config.development.ApplicationMode
import com.ruchij.config.development.ApplicationMode.{DockerCompose, Local, Production}
import com.ruchij.email.{ConsoleEmailService, EmailService, SendGridEmailService}
import com.ruchij.messaging.Subscriber
import com.ruchij.messaging.kafka.KafkaSubscriber
import com.sendgrid.SendGrid
import pureconfig.{ConfigObjectSource, ConfigReader}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

case class EmailServiceDependencies[F[_]](emailService: EmailService[F], subscriber: Subscriber[F])

object EmailServiceDependencies {
  def from[F[_]: Sync: ContextShift: Future ~> *[_], G[_]: Sync: ConfigReader.Result ~> *[_]](
    applicationMode: ApplicationMode,
    configObjectSource: ConfigObjectSource
  )(implicit actorSystem: ActorSystem): G[EmailServiceDependencies[F]] =
    applicationMode match {
      case Production =>
        for {
          kafkaClientConfiguration <- KafkaClientConfiguration.confluent[G](configObjectSource)
          kafkaSubscriber = new KafkaSubscriber[F](kafkaClientConfiguration)

          sendgridConfiguration <- SendgridConfiguration.load[G](configObjectSource)
          ioBlockingExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
          sendgridEmailService = new SendGridEmailService[F](
            new SendGrid(sendgridConfiguration.sendgridApiKey),
            ioBlockingExecutionContext
          )
        } yield EmailServiceDependencies(sendgridEmailService, kafkaSubscriber)

      case DockerCompose =>
        for {
          kafkaClientConfiguration <- KafkaClientConfiguration.local[G](configObjectSource)
          kafkaSubscriber = new KafkaSubscriber[F](kafkaClientConfiguration)
        }
        yield EmailServiceDependencies(new ConsoleEmailService[F], kafkaSubscriber)

      case Local =>
        Applicative[G].pure {
          EmailServiceDependencies(
            new ConsoleEmailService[F],
            ???
          )
        }
    }
}
