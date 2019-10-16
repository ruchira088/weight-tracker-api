package com.ruchij.test.stubs

import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.effect.Clock
import org.joda.time.DateTime

import scala.concurrent.duration.TimeUnit
import scala.language.higherKinds

class FlexibleClock[F[_]: Applicative](private var dateTime: DateTime) extends Clock[F] {

  override def realTime(unit: TimeUnit): F[Long] =
    Applicative[F].pure(unit.convert(dateTime.getMillis, TimeUnit.MILLISECONDS))

  override def monotonic(unit: TimeUnit): F[Long] = realTime(unit)

  def setDateTime(newValue: DateTime): Unit =
    dateTime = newValue
}
