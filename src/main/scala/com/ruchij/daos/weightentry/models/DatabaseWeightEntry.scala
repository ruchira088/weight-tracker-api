package com.ruchij.daos.weightentry.models

import java.util.UUID

import org.joda.time.DateTime

case class DatabaseWeightEntry(
  id: UUID,
  index: Long,
  createdAt: DateTime,
  createdBy: UUID,
  userId: UUID,
  weight: Double,
  description: Option[String]
)
