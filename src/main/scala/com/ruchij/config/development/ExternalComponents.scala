package com.ruchij.config.development

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import cats.effect.{Async, ContextShift, Sync}
import cats.implicits._
import cats.{Applicative, ~>}
import com.ruchij.config.development.ApplicationMode.{DockerCompose, Local, Production}
import com.ruchij.config.{DoobieConfiguration, EmailConfiguration, RedisConfiguration}
import com.ruchij.migration.MigrationApp
import com.ruchij.migration.config.DatabaseConfiguration
import com.ruchij.services.email.{ConsoleEmailService, EmailService, SendGridEmailService}
import com.sendgrid.SendGrid
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import pureconfig.{ConfigObjectSource, ConfigReader}
import redis.RedisClient
import redis.embedded.RedisServer
import redis.embedded.ports.EphemeralPortProvider

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.language.higherKinds

case class ExternalComponents[F[_]](
  redisClient: RedisClient,
  transactor: Transactor.Aux[F, Unit],
  emailService: EmailService[F],
  shutdownHook: () => F[Unit]
)

object ExternalComponents {
  lazy val ioBlockingExecutionContext: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  def from[G[_]: Sync, F[_]: Async: ContextShift](
    applicationMode: ApplicationMode,
    configObjectSource: ConfigObjectSource
  )(implicit actorSystem: ActorSystem, functionK: ConfigReader.Result ~> G): G[ExternalComponents[F]] =
    applicationMode match {
      case Production =>
        for {
          redisConfiguration <- RedisConfiguration.load[G](configObjectSource)
          doobieConfiguration <- DoobieConfiguration.load[G](configObjectSource)
          emailConfiguration <- EmailConfiguration.load[G](configObjectSource)
        } yield
          ExternalComponents[F](
            redisClient(redisConfiguration),
            doobieTransactor[F](doobieConfiguration),
            new SendGridEmailService[F](new SendGrid(emailConfiguration.sendgridApiKey), ioBlockingExecutionContext),
            () => Applicative[F].unit
          )

      case DockerCompose =>
        for {
          redisConfiguration <- RedisConfiguration.load[G](configObjectSource)
          doobieConfiguration <- DoobieConfiguration.load[G](configObjectSource)
        }
        yield
          ExternalComponents(
            redisClient(redisConfiguration),
            doobieTransactor(doobieConfiguration),
            new ConsoleEmailService[F],
            () => Applicative[F].unit
          )

      case Local => local[G, F]()
    }

  def local[G[_]: Sync, F[_]: Async: ContextShift]()(implicit actorSystem: ActorSystem): G[ExternalComponents[F]] =
    for {
      (redisServer, redisPort) <- startEmbeddedRedisServer[G]
      _ <- MigrationApp.migrate[G](H2_DATABASE_CONFIGURATION)
    } yield
      ExternalComponents(
        redisClient(RedisConfiguration("localhost", redisPort, None)),
        h2Transactor[F],
        new ConsoleEmailService[F],
        () => Sync[F].delay(redisServer.stop())
      )

  val H2_DATABASE_CONFIGURATION: DatabaseConfiguration =
    DatabaseConfiguration(
      "jdbc:h2:mem:weight-tracker;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "",
      ""
    )

  def h2Transactor[F[_]: Async: ContextShift]: Aux[F, Unit] =
    doobieTransactor {
      DoobieConfiguration(
        "org.h2.Driver",
        H2_DATABASE_CONFIGURATION.url,
        H2_DATABASE_CONFIGURATION.user,
        H2_DATABASE_CONFIGURATION.password
      )
    }

  def doobieTransactor[F[_]: Async: ContextShift](doobieConfiguration: DoobieConfiguration): Aux[F, Unit] =
    Transactor.fromDriverManager[F](
      doobieConfiguration.driver,
      doobieConfiguration.url,
      doobieConfiguration.user,
      doobieConfiguration.password
    )

  def redisClient(redisConfiguration: RedisConfiguration)(implicit actorSystem: ActorSystem): RedisClient =
    RedisClient(redisConfiguration.host, redisConfiguration.port, redisConfiguration.password)

  def startEmbeddedRedisServer[F[_]: Sync]: F[(RedisServer, Int)] =
    for {
      redisPort <- Sync[F].delay(new EphemeralPortProvider().next())
      redisServer = new RedisServer(redisPort)
      _ <- Sync[F].delay(redisServer.start())

    } yield (redisServer, redisPort)
}
