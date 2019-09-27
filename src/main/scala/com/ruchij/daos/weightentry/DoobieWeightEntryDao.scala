package com.ruchij.daos.weightentry

import java.util.UUID

import cats.implicits._
import cats.data.OptionT
import cats.effect.Sync
import com.ruchij.daos.weightentry.models.DatabaseWeightEntry
import com.ruchij.exceptions.InternalServiceException
import doobie.util.transactor.Transactor
import com.ruchij.daos.doobie.DoobieCustomMappings._
import doobie.postgres.implicits._
import doobie.implicits._

import scala.language.higherKinds

class DoobieWeightEntryDao[F[_]: Sync](transactor: Transactor.Aux[F, Unit]) extends WeightEntryDao[F] {

  override def insert(databaseWeightEntry: DatabaseWeightEntry): F[DatabaseWeightEntry] =
    sql"""
        insert into weight_entry (id, created_at, created_by, user_id, weight, description)
          values (
            ${databaseWeightEntry.id},
            ${databaseWeightEntry.createdAt},
            ${databaseWeightEntry.createdBy},
            ${databaseWeightEntry.userId},
            ${databaseWeightEntry.weight},
            ${databaseWeightEntry.description}
          )
      """
        .update
        .run
        .transact(transactor)
        .flatMap {
          _ =>
            findById(databaseWeightEntry.id)
              .getOrElseF(Sync[F].raiseError(InternalServiceException("Unable to persist weight entry")))
        }

  override def findById(id: UUID): OptionT[F, DatabaseWeightEntry] =
    OptionT {
      sql"""
        select id, index, created_at, created_by, user_id, weight, description from
          weight_entry where id = $id
       """
        .query[DatabaseWeightEntry]
        .option
        .transact(transactor)
    }

  override def findByUser(userId: UUID): F[List[DatabaseWeightEntry]] =
    sql"""
        select id, index, created_at, created_by, user_id, weight, description from 
          weight_entry where user_id = $userId
    """
      .query[DatabaseWeightEntry]
      .to[List]
      .transact(transactor)
}
