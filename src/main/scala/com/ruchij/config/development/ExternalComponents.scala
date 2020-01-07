package com.ruchij.config.development

import java.nio.file.Paths

import akka.actor.ActorSystem
import cats.effect.{Async, ContextShift, Sync}
import cats.implicits._
import cats.{Applicative, ~>}
import com.ruchij.config.development.ApplicationMode.{DockerCompose, Production}
import com.ruchij.config.{DoobieConfiguration, KafkaClientConfiguration, RedisConfiguration}
import com.ruchij.messaging.Publisher
import com.ruchij.messaging.file.FileBasedPublisher
import com.ruchij.messaging.inmemory.InMemoryPublisher
import com.ruchij.messaging.kafka.KafkaProducer
import com.ruchij.migration.MigrationApp
import com.ruchij.migration.config.DatabaseConfiguration
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import pureconfig.{ConfigObjectSource, ConfigReader}
import redis.RedisClient
import redis.embedded.RedisServer
import redis.embedded.ports.EphemeralPortProvider

import scala.language.higherKinds

case class ExternalComponents[F[_]](
  redisClient: RedisClient,
  transactor: Transactor.Aux[F, Unit],
  publisher: Publisher[F, _],
  shutdownHook: F[Unit]
)

object ExternalComponents {

  def from[G[_]: Sync, F[_]: Async: ContextShift](
    applicationMode: ApplicationMode,
    configObjectSource: ConfigObjectSource
  )(implicit actorSystem: ActorSystem, functionK: ConfigReader.Result ~> G): G[ExternalComponents[F]] =
    applicationMode match {
      case Production =>
        for {
          redisConfiguration <- RedisConfiguration.load[G](configObjectSource)
          doobieConfiguration <- DoobieConfiguration.load[G](configObjectSource)
          kafkaClientConfiguration <- KafkaClientConfiguration.confluent[G](configObjectSource)
        } yield
          ExternalComponents[F](
            redisClient(redisConfiguration),
            doobieTransactor[F](doobieConfiguration),
            new KafkaProducer[F](kafkaClientConfiguration),
            Applicative[F].unit
          )

      case DockerCompose =>
        for {
          redisConfiguration <- RedisConfiguration.load[G](configObjectSource)
          doobieConfiguration <- DoobieConfiguration.load[G](configObjectSource)
          kafkaClientConfiguration <- KafkaClientConfiguration.local[G](configObjectSource)
        } yield
          ExternalComponents(
            redisClient(redisConfiguration),
            doobieTransactor(doobieConfiguration),
            new KafkaProducer[F](kafkaClientConfiguration),
            Applicative[F].unit
          )

      case _ => slim[G, F](applicationMode)
    }

  def slim[G[_]: Sync, F[_]: Async: ContextShift](applicationMode: ApplicationMode)(implicit actorSystem: ActorSystem): G[ExternalComponents[F]] =
    for {
      (redisServer, redisPort) <- startEmbeddedRedisServer[G]
      _ <- MigrationApp.migrate[G](H2_DATABASE_CONFIGURATION)
    } yield
      ExternalComponents(
        redisClient(RedisConfiguration("localhost", redisPort, None)),
        h2Transactor[F],
        if (applicationMode == ApplicationMode.Local)
          new FileBasedPublisher[F](Paths.get("./file-based-messaging.txt"))
        else
          InMemoryPublisher.empty[F],
        Sync[F].delay(redisServer.stop())
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
