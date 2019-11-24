package com.ruchij.logging.models

import ch.qos.logback.classic.Level
import com.ruchij.circe.Encoders.{jodaTimeEncoder, taggedStringEncoder}
import com.ruchij.web.headers.`X-Correlation-ID`.CorrelationId
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.joda.time.DateTime

case class LoggingEvent(
  timestamp: DateTime,
  loggerName: String,
  level: Level,
  message: String,
  threadName: String,
  correlationId: Option[CorrelationId]
)

object LoggingEvent {
  implicit val levelEncoder: Encoder[Level] = Encoder.encodeString.contramap[Level](_.levelStr)

  implicit val loggingEventEncoder: Encoder[LoggingEvent] = deriveEncoder[LoggingEvent]
}
