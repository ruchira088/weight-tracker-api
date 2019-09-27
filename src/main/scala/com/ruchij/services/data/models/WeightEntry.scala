package com.ruchij.services.data.models

import java.util.UUID

import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import org.joda.time.DateTime

case class WeightEntry(
  id: UUID,
  createdAt: DateTime,
  createdBy: UUID,
  userId: UUID,
  weight: Double,
  description: Option[String]
)

object WeightEntry {
  def fromDatabaseWeightEntry(databaseWeightEntry: DatabaseWeightEntry): WeightEntry =
    WeightEntry(
      databaseWeightEntry.id,
      databaseWeightEntry.createdAt,
      databaseWeightEntry.createdBy,
      databaseWeightEntry.userId,
      databaseWeightEntry.weight,
      databaseWeightEntry.description
    )
}
