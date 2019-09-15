package com.ruchij.services.health

import java.util.concurrent.TimeUnit

import cats.Functor
import cats.effect.Clock
import cats.implicits.toFunctorOps
import com.ruchij.services.health.models.ServiceInformation
import org.joda.time.DateTime

import scala.language.higherKinds

class HealthCheckServiceImpl[F[_]: Clock: Functor] extends HealthCheckService[F] {

  override def serviceInformation(): F[ServiceInformation] =
    Clock[F].realTime(TimeUnit.MILLISECONDS)
      .map { timestamp => ServiceInformation(new DateTime(timestamp)) }
}
