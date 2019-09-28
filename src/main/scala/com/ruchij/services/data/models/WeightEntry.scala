package com.ruchij.services.data.models

import java.util.UUID

import cats.Applicative
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import com.ruchij.circe.Encoders.jodaTimeEncoder
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import org.joda.time.DateTime

import scala.language.higherKinds

case class WeightEntry(
  id: UUID,
  userId: UUID,
  timestamp: DateTime,
  weight: Double,
  description: Option[String]
)

object WeightEntry {
  def fromDatabaseWeightEntry(databaseWeightEntry: DatabaseWeightEntry): WeightEntry =
    WeightEntry(
      databaseWeightEntry.id,
      databaseWeightEntry.userId,
      databaseWeightEntry.timestamp,
      databaseWeightEntry.weight,
      databaseWeightEntry.description
    )

  implicit def weightEntryEncoder[F[_]: Applicative]: EntityEncoder[F, WeightEntry] = jsonEncoderOf[F, WeightEntry]
}
