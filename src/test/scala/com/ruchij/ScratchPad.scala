package com.ruchij

import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.{Path, Paths, StandardOpenOption}

import cats.effect.{ExitCode, IO, IOApp}
import com.ruchij.services.email.SendGridEmailService
import com.ruchij.services.email.models.Email
import com.ruchij.test.utils.RandomGenerator
import com.sendgrid.SendGrid

import scala.concurrent.ExecutionContext
import scala.util.Try

object ScratchPad extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      email <- IO.delay(Email.welcomeEmail(RandomGenerator.user()))

      sendgridApiKey <- environmentValue("SENDGRID_API_KEY")
      sendgridEmailService = new SendGridEmailService[IO](new SendGrid(sendgridApiKey), ExecutionContext.global)
      _ <- sendgridEmailService.send(email)

      _ <- writeToFile(Paths.get("welcome.html"), email.content.body.getBytes)
    }
    yield ExitCode.Success

  def environmentValue(name: String): IO[String] =
    IO.suspend {
      sys.env.get(name)
        .fold[IO[String]](IO.raiseError(new NoSuchElementException(s"Unable to find $name as an environment variable")))(IO.pure)
    }

  def writeToFile(path: Path, content: Array[Byte]): IO[Int] =
    IO.async[Int] { cb =>
      val fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)

      fileChannel.write(ByteBuffer.wrap(content), 0, (): Unit, new CompletionHandler[Integer, Unit] {
        override def completed(result: Integer, attachment: Unit): Unit =
          Try(fileChannel.close())
            .fold(throwable => cb(Left(throwable)), _ => cb(Right(result)))

        override def failed(throwable: Throwable, attachment: Unit): Unit = {
          cb(Left(throwable))
          fileChannel.close()
        }
      })
    }
}
