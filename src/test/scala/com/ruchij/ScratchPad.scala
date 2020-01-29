package com.ruchij

import cats.effect.{ExitCode, IO, IOApp}
import com.ruchij.services.resource.S3ResourceService
import com.ruchij.types.FunctionKTypes._
import software.amazon.awssdk.services.s3.S3AsyncClient

object ScratchPad extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      s3AsyncClient <- IO.delay(S3AsyncClient.create())
      s3ResourceService = new S3ResourceService[IO](s3AsyncClient, "resources.weight-tracker.ruchij.com", "")

      result <- s3ResourceService.delete("health-check/60d731b1-cde1-47da-9478-f6b682b86ecb-favicon.ico").value
      _ <- IO.delay(println(result))
    }
    yield ExitCode.Success
}
