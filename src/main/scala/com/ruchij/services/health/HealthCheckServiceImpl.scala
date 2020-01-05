package com.ruchij.services.health

import java.util.concurrent.TimeUnit

import cats.effect.{Clock, Sync}
import cats.implicits._
import cats.~>
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.config.BuildInformation
import com.ruchij.config.development.ApplicationMode
import com.ruchij.messaging.Publisher
import com.ruchij.messaging.models.{HealthCheckProbe, Message}
import com.ruchij.services.health.models.{HealthStatus, ServiceInformation}
import com.ruchij.web.responses.HealthCheckResponse
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.joda.time.DateTime
import redis.RedisClient

import scala.concurrent.Future
import scala.language.higherKinds

class HealthCheckServiceImpl[F[_]: Clock: Sync](
  transactor: Transactor.Aux[F, Unit],
  redisClient: RedisClient,
  publisher: Publisher[F, _],
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
    Clock[F].realTime(TimeUnit.MILLISECONDS)
      .flatMap {
        timestamp =>
          Sync[F].handleError[HealthStatus] {
            publisher.publish { Message(HealthCheckProbe(BuildInfo.name, new DateTime(timestamp), None)) }
              .as(HealthStatus.Healthy)
          }(_ => HealthStatus.Unhealthy)
      }

  override def healthCheck(): F[HealthCheckResponse] =
    for {
      databaseStatus <- database()
      redisStatus <- redis()
      publisherStatus <- publisher()
    }
    yield HealthCheckResponse(databaseStatus, redisStatus, publisherStatus)
}
