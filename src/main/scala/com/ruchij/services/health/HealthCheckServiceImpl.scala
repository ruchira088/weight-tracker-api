package com.ruchij.services.health

import java.util.concurrent.TimeUnit

import cats.data.OptionT
import cats.effect.{Blocker, Clock, ContextShift, Sync}
import cats.implicits._
import cats.{Applicative, ~>}
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.config.BuildInformation
import com.ruchij.config.development.ApplicationMode
import com.ruchij.exceptions.ResourceNotFoundException
import com.ruchij.messaging.Publisher
import com.ruchij.messaging.models.{HealthCheckProbe, Message}
import com.ruchij.services.health.models.{HealthStatus, ServiceInformation}
import com.ruchij.services.resource.ResourceService
import com.ruchij.services.resource.models.Resource
import com.ruchij.web.responses.HealthCheckResponse
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.http4s.{MediaType, StaticFile}
import org.joda.time.DateTime
import redis.RedisClient

import scala.concurrent.Future
import scala.language.higherKinds

class HealthCheckServiceImpl[F[_]: Clock: Sync: ContextShift](
  transactor: Transactor.Aux[F, Unit],
  redisClient: RedisClient,
  publisher: Publisher[F, _],
  resourceService: ResourceService[F],
  ioBlocker: Blocker,
  applicationMode: ApplicationMode,
  buildInformation: BuildInformation
)(implicit functionK: Future ~> F)
    extends HealthCheckService[F] {

  override def serviceInformation(): F[ServiceInformation] =
    Clock[F]
      .realTime(TimeUnit.MILLISECONDS)
      .map { timestamp =>
        ServiceInformation(applicationMode, new DateTime(timestamp), buildInformation)
      }

  def database(): F[HealthStatus] =
    Sync[F].handleError[HealthStatus] {
      sql"select 1"
        .query[Int]
        .option
        .transact(transactor)
        .map {
          case Some(1) => HealthStatus.Healthy
          case _ => HealthStatus.Unhealthy
        }
    }(_ => HealthStatus.Unhealthy)

  def redis(): F[HealthStatus] =
    Sync[F].handleError[HealthStatus] {
      Sync[F].suspend(functionK(redisClient.ping())).as(HealthStatus.Healthy)
    }(_ => HealthStatus.Unhealthy)

  def publisher(): F[HealthStatus] =
    Clock[F]
      .realTime(TimeUnit.MILLISECONDS)
      .flatMap { timestamp =>
        Sync[F].handleError[HealthStatus] {
          publisher
            .publish { Message(HealthCheckProbe(BuildInfo.name, new DateTime(timestamp), None)) }
            .as(HealthStatus.Healthy)
        }(_ => HealthStatus.Unhealthy)
      }

  def resourceStorage(): F[HealthStatus] =
    for {
      iconUrlOpt <- Sync[F].delay(Option(getClass.getClassLoader.getResource("favicon.ico")))

      data <- OptionT
        .fromOption[F](iconUrlOpt)
        .flatMap { url =>
          StaticFile.fromURL[F](url, ioBlocker).map(_.body)
        }
        .getOrElseF {
          Sync[F].raiseError(ResourceNotFoundException("Unable to load favicon.ico from class loader resources"))
        }

      resource = Resource(MediaType.image.`x-icon`, data)

      timestamp <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      key = s"health-check/$timestamp-favicon.ico"

      _ <- resourceService.insert(key, resource)

      fetchedResource <- resourceService.fetch(key).value
      fetchedResourceBytes <- OptionT.fromOption[F](fetchedResource).semiflatMap(_.data.compile.toList).value

      deletionSuccess <- resourceService.delete(key).isDefined

      resourceBytes <- resource.data.compile.toList

      healthStatus = if (fetchedResourceBytes.contains(resourceBytes) && deletionSuccess
        && fetchedResource.exists(_.contentType == resource.contentType))
        HealthStatus.Healthy
      else
        HealthStatus.Unhealthy

    } yield healthStatus

  override def healthCheck(): F[HealthCheckResponse] =
    for {
      databaseStatus <- database()
      redisStatus <- redis()
      publisherStatus <- publisher()
      resourceStorageStatus <- resourceStorage()
    } yield HealthCheckResponse(databaseStatus, redisStatus, publisherStatus, resourceStorageStatus)
}
