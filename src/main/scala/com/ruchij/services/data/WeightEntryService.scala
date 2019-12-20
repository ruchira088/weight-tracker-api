package com.ruchij.services.data

import java.util.UUID

import com.ruchij.services.data.models.WeightEntry
import com.ruchij.types.Tags.{PageNumber, PageSize}
import org.joda.time.DateTime

import scala.language.higherKinds

trait WeightEntryService[F[_]] {
  def create(
    timestamp: DateTime,
    weight: Double,
    description: Option[String],
    userId: UUID,
    createdBy: UUID
  ): F[WeightEntry]

  def getById(id: UUID): F[WeightEntry]

  def findByUser(userId: UUID, pageNumber: PageNumber, pageSize: PageSize): F[List[WeightEntry]]

  def delete(id: UUID): F[WeightEntry]
}
