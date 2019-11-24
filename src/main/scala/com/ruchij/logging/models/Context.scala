package com.ruchij.logging.models

import com.ruchij.web.headers.`X-Correlation-ID`.CorrelationId

import scala.language.implicitConversions

case class Context(correlationId: CorrelationId)

object Context {
  implicit def fromCorrelationId(correlationId: CorrelationId): Context = Context(correlationId)
}
