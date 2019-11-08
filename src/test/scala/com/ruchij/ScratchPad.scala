package com.ruchij

import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.{Path, Paths, StandardOpenOption}

import cats.data.OptionT
import cats.effect.{Blocker, ExitCode, IO, IOApp, Sync}
import com.ruchij.services.email.SendGridEmailService
import com.ruchij.services.email.models.Email
import com.ruchij.test.utils.RandomGenerator
import com.sendgrid.SendGrid

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object ScratchPad extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      email <- IO.delay(Email.welcomeEmail(RandomGenerator.user()))

//      sendgridApiKey <- environmentValue("SENDGRID_API_KEY")
//      sendgridEmailService = new SendGridEmailService[IO](new SendGrid(sendgridApiKey), ExecutionContext.global)
//      _ <- sendgridEmailService.send(email)
//
      filePath = Paths.get("welcome.html")

      _ <- deleteFile(filePath)(ExecutionContext.global).value
      _ <- writeToFile(filePath, email.content.body.getBytes)

    } yield ExitCode.Success

  def environmentValue(name: String): IO[String] =
    IO.suspend {
      sys.env
        .get(name)
        .fold[IO[String]](
          IO.raiseError(new NoSuchElementException(s"Unable to find $name as an environment variable"))
        )(IO.pure)
    }

  def deleteFile(path: Path)(implicit ioBlockingExecutionContext: ExecutionContext): OptionT[IO, Boolean] =
    OptionT {
      Blocker
        .liftExecutionContext(ioBlockingExecutionContext)
        .blockOn {
          IO.delay(path.toFile.exists())
            .flatMap { exists =>
              if (exists) IO.delay(path.toFile.delete()).map(Some.apply) else IO.pure(None)
            }
        }
    }

  def writeToFile(path: Path, content: Array[Byte]): IO[Int] =
    IO.delay {
        AsynchronousFileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
      }
      .bracket {
        fileChannel => IO.async[Int] { callback =>
          fileChannel.write(ByteBuffer.wrap(content), 0, (): Unit, new CompletionHandler[Integer, Unit] {
            override def completed(result: Integer, attachment: Unit): Unit =
              callback(Right(result))

            override def failed(throwable: Throwable, attachment: Unit): Unit =
              callback(Left(throwable))
          })
        }
      } (fileChannel => IO.delay(fileChannel.close()))
}
