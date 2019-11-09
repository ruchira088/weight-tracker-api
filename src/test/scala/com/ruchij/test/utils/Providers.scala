package com.ruchij.test.utils

import java.util.concurrent.TimeUnit

import cats.effect.{Clock, ContextShift, IO, Sync}
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object Providers {
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  implicit def clock[F[_]: Sync]: Clock[F] = stubClock(DateTime.now())

  def stubClock[F[_]: Sync](dateTime: => DateTime): Clock[F] =
    new Clock[F] {
      override def realTime(unit: TimeUnit): F[Long] =
        Sync[F].delay(unit.convert(dateTime.getMillis, TimeUnit.MILLISECONDS))

      override def monotonic(unit: TimeUnit): F[Long] = realTime(unit)
    }
}
