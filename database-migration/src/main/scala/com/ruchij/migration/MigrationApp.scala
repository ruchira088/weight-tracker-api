package com.ruchij.migration

import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.implicits._
import com.ruchij.migration.config.{DatabaseConfiguration, MigrationConfiguration}
import org.flywaydb.core.Flyway

import scala.language.higherKinds

object MigrationApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      migrationConfiguration <- IO.fromEither(MigrationConfiguration.load())

      result <- migrate[IO](migrationConfiguration.databaseConfiguration)

      _ <- IO(println(s"Migration result: $result"))
    }
    yield ExitCode.Success

  def migrate[F[_]: Sync](databaseConfiguration: DatabaseConfiguration): F[Int] =
    for {
      flyway <- Sync[F].delay {
        Flyway.configure().dataSource(databaseConfiguration.url, databaseConfiguration.user, databaseConfiguration.password)
          .load()
      }

      result <- Sync[F].delay(flyway.migrate())
    }
    yield result
}
