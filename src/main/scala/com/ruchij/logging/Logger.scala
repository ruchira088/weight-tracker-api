package com.ruchij.logging

import cats.effect.Sync
import org.log4s.{Logger => Log4sLogger}

import scala.language.higherKinds

object Logger {
  private def enhanceMessage(message: String, context: Context): String =
    s"[correlationId=${context.correlationId}] $message"

  implicit class LoggerOps(logger: Log4sLogger) {
    def traceF[F[_]: Sync](message: String)(context: Context): F[Unit] =
      Sync[F].delay {
        logger.trace(enhanceMessage(message, context))
      }

    def debugF[F[_]: Sync](message: String)(context: Context): F[Unit] =
      Sync[F].delay {
        logger.debug(enhanceMessage(message, context))
      }

    def infoF[F[_]: Sync](message: String)(context: Context): F[Unit] =
      Sync[F].delay {
        logger.info(enhanceMessage(message, context))
      }

    def errorF[F[_]: Sync](throwable: Throwable)(message: String)(context: Context): F[Unit] =
      Sync[F].delay {
        logger.error(throwable)(enhanceMessage(message, context))
      }
  }
}
