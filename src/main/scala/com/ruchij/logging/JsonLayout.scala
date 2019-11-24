package com.ruchij.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.LayoutBase
import com.ruchij.logging.models.{Context, LoggingEvent}
import io.circe.syntax._
import org.joda.time.DateTime

class JsonLayout extends LayoutBase[ILoggingEvent] {
  override def doLayout(event: ILoggingEvent): String = {
    val contextOption = event.getArgumentArray.headOption.collect { case context: Context => context }

    LoggingEvent(
      new DateTime(event.getTimeStamp),
      event.getLoggerName,
      event.getLevel,
      event.getFormattedMessage,
      event.getThreadName,
      contextOption.map(_.correlationId)
    )
      .asJson
      .dropNullValues
      .noSpacesSortKeys + "\n"
  }
}
