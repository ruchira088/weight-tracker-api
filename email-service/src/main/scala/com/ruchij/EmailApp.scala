package com.ruchij

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.{ExitCode, IO, IOApp}
import com.ruchij.config.KafkaClientConfiguration
import com.ruchij.email.{ConsoleEmailService, EmailSubscriber}
import com.ruchij.messaging.kafka.KafkaSubscriber
import com.ruchij.types.FunctionKTypes._
import pureconfig.ConfigSource

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

object EmailApp extends IOApp {
  implicit val actorSystem: ActorSystem = ActorSystem("email-service")

  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.default)

      kafkaClientConfiguration <- KafkaClientConfiguration.local[IO](configObjectSource)

      kafkaSubscriber = new KafkaSubscriber[IO](kafkaClientConfiguration)
      emailService = new ConsoleEmailService[IO]

      _ <- EmailSubscriber.run(kafkaSubscriber, emailService)
    }
    yield ExitCode.Success
}
