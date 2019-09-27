package com.ruchij.daos.user

import java.util.UUID

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits.toFlatMapOps
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.exceptions.InternalServiceException
import doobie.postgres.implicits._
import com.ruchij.daos.doobie.DoobieCustomMappings._
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.language.higherKinds

class DoobieUserDao[F[_]: Sync](transactor: Transactor.Aux[F, Unit]) extends UserDao[F] {

  override def insert(databaseUser: DatabaseUser): F[DatabaseUser] =
    sql"""
      insert into users (id, created_at, username, password, email, first_name, last_name)
        values (
          ${databaseUser.id},
          ${databaseUser.createdAt},
          ${databaseUser.username},
          ${databaseUser.password},
          ${databaseUser.email},
          ${databaseUser.firstName},
          ${databaseUser.lastName}
        )
      """.update.run
      .transact(transactor)
      .flatMap { _ =>
        findById(databaseUser.id)
          .getOrElseF(Sync[F].raiseError(InternalServiceException("Unable to persist user")))
      }

  override def findById(id: UUID): OptionT[F, DatabaseUser] =
    OptionT {
      sql"select id, created_at, username, password, email, first_name, last_name from users where id = $id"
        .query[DatabaseUser]
        .option
        .transact(transactor)
    }

  override def findByUsername(username: String): OptionT[F, DatabaseUser] =
    OptionT {
      sql"select id, created_at, username, password, email, first_name, last_name from users where username = $username"
        .query[DatabaseUser]
        .option
        .transact(transactor)
    }

  override def findByEmail(email: String): OptionT[F, DatabaseUser] =
    OptionT {
      sql"select id, created_at, username, password, email, first_name, last_name from users where email = $email"
        .query[DatabaseUser]
        .option
        .transact(transactor)
    }
}
