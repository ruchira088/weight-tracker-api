package com.ruchij.messaging.models

import org.joda.time.DateTime

case class HealthCheckProbe(service: String, probedAt: DateTime, instanceId: Option[String])
