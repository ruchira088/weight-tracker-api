package com.ruchij.test.utils

import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.effect.{Clock, ContextShift, IO}
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object Providers {
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  implicit val clock: Clock[IO] = stubClock(DateTime.now())

  def stubClock[F[_]: Applicative](dateTime: => DateTime): Clock[F] =
    new Clock[F] {
      override def realTime(unit: TimeUnit): F[Long] =
        Applicative[F].pure(unit.convert(dateTime.getMillis, TimeUnit.MILLISECONDS))

      override def monotonic(unit: TimeUnit): F[Long] = realTime(unit)
    }
}
