package com.ruchij

import java.nio.file.Path

import cats.data.OptionT
import cats.effect.{Blocker, ContextShift, ExitCode, IO, IOApp, Sync}
import cats.implicits._
import com.ruchij.test.utils.RandomGenerator

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object ScratchPad extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    IO(println(RandomGenerator.databaseUser())).as(ExitCode.Success)
//    for {
//      email <- IO.delay(Email.welcomeEmail(RandomGenerator.user()))
//
//      sendgridApiKey <- environmentValue("SENDGRID_API_KEY")
//      sendgridEmailService = new SendGridEmailService[IO](new SendGrid(sendgridApiKey), ExecutionContext.global)
//      _ <- sendgridEmailService.send(email)
//
//      filePath = Paths.get("welcome.html")
//      _ <- deleteFile[IO](filePath, ExecutionContext.global).value
//      _ <- writeToFile[IO](filePath, email.content.body.getBytes)
//
//    } yield ExitCode.Success

  def environmentValue[F[_]: Sync](name: String): F[String] =
    Sync[F].suspend {
      sys.env
        .get(name)
        .fold[F[String]](
          Sync[F].raiseError(new NoSuchElementException(s"Unable to find $name as an environment variable"))
        )(Sync[F].pure)
    }

  def deleteFile[F[_]: Sync: ContextShift](path: Path, ioBlockingExecutionContext: ExecutionContext): OptionT[F, Boolean] =
    OptionT {
      Blocker
        .liftExecutionContext(ioBlockingExecutionContext)
        .blockOn {
          Sync[F].delay(path.toFile.exists())
            .flatMap { exists =>
              if (exists) Sync[F].delay(path.toFile.delete()).map(Some.apply) else Sync[F].pure(None)
            }
        }
    }
}
