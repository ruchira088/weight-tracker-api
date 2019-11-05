package com.ruchij

import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.{Path, Paths, StandardOpenOption}

import cats.effect.{ContextShift, IO}
import com.ruchij.services.email.SendGridEmailService
import com.ruchij.services.email.models.Email
import com.ruchij.services.email.models.Email.EmailAddressTag
import com.ruchij.test.utils.RandomGenerator
import com.sendgrid.SendGrid
import shapeless.tag
import html.Welcome
import play.twirl.api.Html

import scala.concurrent.ExecutionContext
import scala.util.Try

object ScratchPad {
  def main(args: Array[String]): Unit = {
    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

    val sendgridApiKey =
      sys.env.getOrElse(
        "SENDGRID_API_KEY",
        throw new Exception("Unable to find SENDGRID_API_KEY as an environment variable")
      )

    new SendGridEmailService[IO](new SendGrid(sendgridApiKey), ExecutionContext.global)
      .send(Email.welcomeEmail(RandomGenerator.user()))
      .unsafeRunSync()

//    writeToFile(Paths.get("welcome.html"), Welcome(RandomGenerator.user()).body.getBytes).unsafeRunSync()
  }

  def writeToFile(path: Path, content: Array[Byte]): IO[Int] =
    IO.async[Int] { cb =>
      val fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)

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
