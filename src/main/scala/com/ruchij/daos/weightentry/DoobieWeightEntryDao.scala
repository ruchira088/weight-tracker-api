package com.ruchij.daos.weightentry

import java.util.UUID

import cats.data.OptionT
import cats.effect.Sync
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import doobie.util.transactor.Transactor
import com.ruchij.daos.doobie.DoobieCustomMappings._
import com.ruchij.daos.doobie.singleUpdate
import com.ruchij.types.Tags.{PageNumber, PageSize}
import doobie.postgres.implicits._
import doobie.implicits._

import scala.language.higherKinds

class DoobieWeightEntryDao[F[_]: Sync](transactor: Transactor.Aux[F, Unit]) extends WeightEntryDao[F] {

  override def insert(databaseWeightEntry: DatabaseWeightEntry): F[Boolean] =
    singleUpdate {
      sql"""
        INSERT INTO weight_entries (id, created_at, created_by, user_id, timestamp, weight, description, deleted)
          VALUES (
            ${databaseWeightEntry.id},
            ${databaseWeightEntry.createdAt},
            ${databaseWeightEntry.createdBy},
            ${databaseWeightEntry.userId},
            ${databaseWeightEntry.timestamp},
            ${databaseWeightEntry.weight},
            ${databaseWeightEntry.description},
            false
          )
      """.update.run
        .transact(transactor)
    }

  override def findById(id: UUID): OptionT[F, DatabaseWeightEntry] =
    OptionT {
      sql"""
        SELECT id, index, created_at, created_by, user_id, timestamp, weight, description FROM
          weight_entries WHERE id = $id AND deleted = false
       """
        .query[DatabaseWeightEntry]
        .option
        .transact(transactor)
    }

  override def findByUser(userId: UUID, pageNumber: PageNumber, pageSize: PageSize): F[List[DatabaseWeightEntry]] =
    sql"""
        SELECT id, index, created_at, created_by, user_id, timestamp, weight, description FROM
          weight_entries WHERE user_id = $userId AND deleted = false LIMIT ${pageSize.toInt} OFFSET ${pageNumber * pageSize}
    """
      .query[DatabaseWeightEntry]
      .to[List]
      .transact(transactor)

  override def delete(id: UUID): F[Boolean] =
    singleUpdate {
      sql"UPDATE weight_entries SET deleted = true WHERE id = $id AND deleted = false"
        .update.run.transact(transactor)
    }
}
