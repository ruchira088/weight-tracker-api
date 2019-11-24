package com.ruchij.logging

import cats.effect.Sync
import com.ruchij.logging.models.Context
import com.typesafe.scalalogging.{Logger => ScalaLogger}

import scala.language.higherKinds
import scala.reflect.ClassTag

class Logger(logger: ScalaLogger) {

  def traceF[F[_]: Sync](message: String)(context: Context): F[Unit] =
    Sync[F].delay {
      logger.trace(message, context)
    }

  def debugF[F[_]: Sync](message: String)(context: Context): F[Unit] =
    Sync[F].delay {
      logger.debug(message, context)
    }

  def infoF[F[_]: Sync](message: String)(context: Context): F[Unit] =
    Sync[F].delay {
      logger.info(message, context)
    }

  def errorF[F[_]: Sync](throwable: Throwable)(context: Context): F[Unit] =
    Sync[F].delay {
      logger.error(throwable.getMessage, context)
    }
}

object Logger {
  def apply[A: ClassTag]: Logger = new Logger(ScalaLogger[A])
}
