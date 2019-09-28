package com.ruchij.daos.weightentry

import java.util.UUID

import cats.data.OptionT
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry

import scala.language.higherKinds

trait WeightEntryDao[F[_]] {
  def insert(databaseWeightEntry: DatabaseWeightEntry): F[Int]

  def findById(id: UUID): OptionT[F, DatabaseWeightEntry]

  def findByUser(userId: UUID): F[List[DatabaseWeightEntry]]
}
