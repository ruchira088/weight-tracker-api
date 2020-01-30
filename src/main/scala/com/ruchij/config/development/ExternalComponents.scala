package com.ruchij.config.development

import java.nio.file.Paths

import akka.actor.ActorSystem
import cats.effect.{Async, Blocker, Clock, Concurrent, ContextShift, Sync}
import cats.implicits._
import cats.{Applicative, ~>}
import com.ruchij.config.development.ApplicationMode.{DockerCompose, Local, Production, Test}
import com.ruchij.config.{DoobieConfiguration, FileResourceConfiguration, KafkaClientConfiguration, RedisConfiguration, S3Configuration}
import com.ruchij.messaging.Publisher
import com.ruchij.messaging.file.FileBasedPublisher
import com.ruchij.messaging.inmemory.InMemoryPublisher
import com.ruchij.messaging.kafka.KafkaProducer
import com.ruchij.migration.MigrationApp
import com.ruchij.migration.config.DatabaseConfiguration
import com.ruchij.services.resource.{FileResourceService, InMemoryResourceService, ResourceService, S3ResourceService}
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import pureconfig.ConfigObjectSource
import pureconfig.ConfigReader.Result
import redis.RedisClient
import redis.embedded.RedisServer
import redis.embedded.ports.EphemeralPortProvider
import software.amazon.awssdk.services.s3.S3AsyncClient

import scala.concurrent.Future
import scala.language.higherKinds

case class ExternalComponents[F[_]](
  redisClient: RedisClient,
  transactor: Transactor.Aux[F, Unit],
  publisher: Publisher[F, _],
  resourceService: ResourceService[F],
  shutdownHook: F[Unit]
)

object ExternalComponents {
  case class TestExternalComponents[F[_]](
    externalComponents: ExternalComponents[F],
    inMemoryPublisher: InMemoryPublisher[F],
    inMemoryResourceService: InMemoryResourceService[F]
  )

  def from[G[_]: Sync: Result ~> *[_], F[_]: Async: ContextShift: Future ~> *[_]: Either[Throwable, *] ~> *[_]: Concurrent: Clock](
    applicationMode: ApplicationMode,
    configObjectSource: ConfigObjectSource,
    blocker: Blocker
  )(implicit actorSystem: ActorSystem): G[ExternalComponents[F]] =
    applicationMode match {
      case Production =>
        for {
          redisConfiguration <- RedisConfiguration.load[G](configObjectSource)
          doobieConfiguration <- DoobieConfiguration.load[G](configObjectSource)
          kafkaClientConfiguration <- KafkaClientConfiguration.confluent[G](configObjectSource)
          s3Configuration <- S3Configuration.load[G](configObjectSource)
        } yield
          ExternalComponents[F](
            redisClient(redisConfiguration),
            doobieTransactor[F](doobieConfiguration),
            new KafkaProducer[F](kafkaClientConfiguration),
            new S3ResourceService[F](S3AsyncClient.create(), s3Configuration.bucket, s3Configuration.prefixKey),
            Applicative[F].unit
          )

      case DockerCompose =>
        for {
          redisConfiguration <- RedisConfiguration.load[G](configObjectSource)
          doobieConfiguration <- DoobieConfiguration.load[G](configObjectSource)
          kafkaClientConfiguration <- KafkaClientConfiguration.local[G](configObjectSource)
          s3Configuration <- S3Configuration.load[G](configObjectSource)
        } yield
          ExternalComponents(
            redisClient(redisConfiguration),
            doobieTransactor(doobieConfiguration),
            new KafkaProducer[F](kafkaClientConfiguration),
            new S3ResourceService[F](S3AsyncClient.create(), s3Configuration.bucket, s3Configuration.prefixKey),
            Applicative[F].unit
          )

      case Local =>
        for {
          (redisServer, redisPort) <- startEmbeddedRedisServer[G]
          _ <- MigrationApp.migrate[G](h2DatabaseConfiguration)
          fileResourceConfiguration <- FileResourceConfiguration.load[G](configObjectSource)
        } yield
          ExternalComponents(
            redisClient(RedisConfiguration("localhost", redisPort, None)),
            h2Transactor[F],
            new FileBasedPublisher[F](Paths.get("./file-based-messaging.txt")),
            new FileResourceService[F](fileResourceConfiguration.fileResourceFolder, fileResourceConfiguration.metaDataFile, blocker),
            Sync[F].delay(redisServer.stop())
          )

      case Test => testComponents[G, F]().map(_.externalComponents)
    }

  def testComponents[G[_]: Sync, F[_]: Async: ContextShift]()(
    implicit actorSystem: ActorSystem
  ): G[TestExternalComponents[F]] =
    for {
      (redisServer, redisPort) <- startEmbeddedRedisServer[G]
      _ <- MigrationApp.migrate[G](h2DatabaseConfiguration)
      inMemoryPublisher = InMemoryPublisher[F]
      inMemoryResourceService = InMemoryResourceService[F]
    } yield
      TestExternalComponents(
        ExternalComponents(
          redisClient(RedisConfiguration("localhost", redisPort, None)),
          h2Transactor[F],
          inMemoryPublisher,
          inMemoryResourceService,
          Sync[F].delay(redisServer.stop())
        ),
        inMemoryPublisher,
        inMemoryResourceService
      )

  val h2DatabaseConfiguration: DatabaseConfiguration =
    DatabaseConfiguration(
      "jdbc:h2:mem:weight-tracker;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "",
      ""
    )

  def h2Transactor[F[_]: Async: ContextShift]: Aux[F, Unit] =
    doobieTransactor {
      DoobieConfiguration(
        "org.h2.Driver",
        h2DatabaseConfiguration.url,
        h2DatabaseConfiguration.user,
        h2DatabaseConfiguration.password
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
