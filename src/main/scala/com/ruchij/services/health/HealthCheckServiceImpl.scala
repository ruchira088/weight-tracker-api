package com.ruchij.services.health

import java.util.concurrent.TimeUnit

import cats.effect.{Clock, Sync}
import cats.implicits._
import cats.~>
import com.ruchij.config.BuildInformation
import com.ruchij.services.health.models.{HealthStatus, ServiceInformation}
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.joda.time.DateTime
import redis.RedisClient

import scala.concurrent.Future
import scala.language.higherKinds

class HealthCheckServiceImpl[F[_]: Clock: Sync](
  transactor: Transactor.Aux[F, Unit],
  redisClient: RedisClient,
  buildInformation: BuildInformation
)(implicit functionK: Future ~> F)
    extends HealthCheckService[F] {

  override def serviceInformation(): F[ServiceInformation] =
    Clock[F]
      .realTime(TimeUnit.MILLISECONDS)
      .map { timestamp =>
        ServiceInformation(new DateTime(timestamp), buildInformation)
      }

  override def database(): F[HealthStatus] =
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

  override def redis(): F[HealthStatus] =
    Sync[F].handleError[HealthStatus] {
      Sync[F].suspend(functionK(redisClient.ping())).as(HealthStatus.Healthy)
    }(_ => HealthStatus.Unhealthy)
}
