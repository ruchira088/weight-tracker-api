package com.ruchij

import cats.implicits._
import cats.effect.{ExitCode, IO, IOApp}
import com.ruchij.config.ServiceConfiguration
import com.ruchij.web.Routes
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      serviceConfiguration <- IO.fromEither(ServiceConfiguration.load())
      exitCode <-
        BlazeServerBuilder[IO]
          .withHttpApp(Routes[IO].orNotFound)
          .bindHttp(serviceConfiguration.httpConfiguration.port)
          .serve.compile.drain.as(ExitCode.Success)
    }
    yield exitCode
}
