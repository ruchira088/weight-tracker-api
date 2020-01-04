package com.ruchij

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.{ExitCode, IO, IOApp}
import com.ruchij.email.EmailSubscriber
import com.ruchij.email.config.{EmailServiceComponents, EmailServiceConfiguration}
import com.ruchij.types.FunctionKTypes._
import pureconfig.ConfigSource

object EmailApp extends IOApp {
  implicit val actorSystem: ActorSystem = ActorSystem("email-service")

  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.default)
      emailServiceConfiguration <- EmailServiceConfiguration.load[IO](configObjectSource)

      emailServiceComponents <-
        EmailServiceComponents.from[IO, IO](emailServiceConfiguration.applicationMode, configObjectSource)

      _ <- EmailSubscriber.run(emailServiceComponents.subscriber, emailServiceComponents.emailService)
    }
    yield ExitCode.Success
}
