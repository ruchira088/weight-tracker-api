package com.ruchij.services.data

import java.util.UUID

import com.ruchij.services.data.models.WeightEntry

import scala.language.higherKinds

trait WeightEntryService[F[_]] {
  def create(weight: Double, description: Option[String], userId: UUID, createdBy: UUID): F[WeightEntry]

  def findByUser(userId: UUID): F[List[WeightEntry]]
}
