package com.ruchij.daos.weightentry

import java.util.UUID

import cats.data.OptionT
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import com.ruchij.types.Tags.{PageNumber, PageSize}

import scala.language.higherKinds

trait WeightEntryDao[F[_]] {
  def insert(databaseWeightEntry: DatabaseWeightEntry): F[Boolean]

  def findById(id: UUID): OptionT[F, DatabaseWeightEntry]

  def findByUser(userId: UUID, pageNumber: PageNumber, pageSize: PageSize): F[List[DatabaseWeightEntry]]

  def delete(id: UUID): F[Boolean]
}
