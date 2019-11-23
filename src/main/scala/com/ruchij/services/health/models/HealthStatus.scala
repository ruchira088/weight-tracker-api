package com.ruchij.services.health.models

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait HealthStatus extends EnumEntry

object HealthStatus extends Enum[HealthStatus] {
  case object Healthy extends HealthStatus
  case object Unhealthy extends HealthStatus

  override def values: immutable.IndexedSeq[HealthStatus] = findValues
}
