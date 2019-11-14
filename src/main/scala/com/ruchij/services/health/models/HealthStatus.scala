package com.ruchij.services.health.models

import cats.Applicative
import com.ruchij.circe.Encoders.enumEncoder
import enumeratum.{Enum, EnumEntry}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

import scala.collection.immutable
import scala.language.higherKinds

sealed trait HealthStatus extends EnumEntry

object HealthStatus extends Enum[HealthStatus] {
  case object Healthy extends HealthStatus
  case object Unhealthy extends HealthStatus

  override def values: immutable.IndexedSeq[HealthStatus] = findValues

  implicit def healthStatusEncoder[F[_]: Applicative]: EntityEncoder[F, HealthStatus] =
    jsonEncoderOf[F, HealthStatus]
}
