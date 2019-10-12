package com.ruchij.migration

import cats.effect.{ExitCode, IO, IOApp}
import com.ruchij.migration.config.{DatabaseConfiguration, MigrationConfiguration}
import org.flywaydb.core.Flyway

object MigrationApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      migrationConfiguration <- IO.fromEither(MigrationConfiguration.load())

      result <- migrate(migrationConfiguration.databaseConfiguration)

      _ <- IO(println(s"Migration result: $result"))
    }
    yield ExitCode.Success

  def migrate(databaseConfiguration: DatabaseConfiguration): IO[Int] =
    for {
      flyway <- IO {
        Flyway.configure().dataSource(databaseConfiguration.url, databaseConfiguration.user, databaseConfiguration.password)
          .load()
      }

      result <- IO(flyway.migrate())
    }
    yield result
}
