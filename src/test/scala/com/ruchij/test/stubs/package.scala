package com.ruchij.test

import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.effect.Clock
import org.joda.time.DateTime

import scala.language.higherKinds

package object stubs {

  def clock[F[_]: Applicative](dateTime: DateTime): Clock[F] =
    new Clock[F] {
      override def realTime(unit: TimeUnit): F[Long] =
        Applicative[F].pure(unit.convert(dateTime.getMillis, TimeUnit.MILLISECONDS))

      override def monotonic(unit: TimeUnit): F[Long] = realTime(unit)
    }
}
