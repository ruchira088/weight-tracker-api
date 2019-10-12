package com.ruchij.daos.weightentry

import java.util.UUID

import cats.data.OptionT
import cats.effect.Sync
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import doobie.util.transactor.Transactor
import com.ruchij.daos.doobie.DoobieCustomMappings._
import com.ruchij.daos.weightentry.WeightEntryDao.{PageNumber, PageSize}
import doobie.postgres.implicits._
import doobie.implicits._

import scala.language.higherKinds

class DoobieWeightEntryDao[F[_]: Sync](transactor: Transactor.Aux[F, Unit]) extends WeightEntryDao[F] {

  override def insert(databaseWeightEntry: DatabaseWeightEntry): F[Int] =
    sql"""
        insert into weight_entry (id, created_at, created_by, user_id, timestamp, weight, description)
          values (
            ${databaseWeightEntry.id},
            ${databaseWeightEntry.createdAt},
            ${databaseWeightEntry.createdBy},
            ${databaseWeightEntry.userId},
            ${databaseWeightEntry.timestamp},
            ${databaseWeightEntry.weight},
            ${databaseWeightEntry.description}
          )
      """.update.run
      .transact(transactor)

  override def findById(id: UUID): OptionT[F, DatabaseWeightEntry] =
    OptionT {
      sql"""
        select id, index, created_at, created_by, user_id, timestamp, weight, description from
          weight_entry where id = $id
       """
        .query[DatabaseWeightEntry]
        .option
        .transact(transactor)
    }

  override def findByUser(userId: UUID, pageNumber: PageNumber, pageSize: PageSize): F[List[DatabaseWeightEntry]] =
    sql"""
        select id, index, created_at, created_by, user_id, timestamp, weight, description from
          weight_entry where user_id = $userId offset ${pageNumber * pageSize} limit ${pageSize.toInt}
    """
      .query[DatabaseWeightEntry]
      .to[List]
      .transact(transactor)
}
