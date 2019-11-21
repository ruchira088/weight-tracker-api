package com.ruchij.logging

import com.ruchij.web.headers.`X-Correlation-ID`.CorrelationId

case class Context(correlationId: CorrelationId)

object Context {
  implicit def fromCorrelationId(correlationId: CorrelationId): Context = Context(correlationId)
}
