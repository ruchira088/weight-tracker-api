package com.ruchij.test.utils

import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.effect.{Clock, ContextShift, IO, Sync}
import com.ruchij.types.Random
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

  def random[F[_]: Applicative, A](result: => A): Random[F, A] =
    new Random[F, A] {
      override def value[B >: A]: F[B] = Applicative[F].pure[B](result)
    }
}
