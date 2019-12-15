package com.ruchij.daos.user

import java.util.UUID

import cats.data.OptionT
import cats.effect.Sync
import com.ruchij.daos.user.models.DatabaseUser
import doobie.postgres.implicits._
import com.ruchij.daos.doobie.DoobieCustomMappings._
import com.ruchij.daos.doobie.singleUpdate
import com.ruchij.services.email.models.Email.EmailAddress
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.language.higherKinds

class DoobieUserDao[F[_]: Sync](transactor: Transactor.Aux[F, Unit]) extends UserDao[F] {

  override def insert(databaseUser: DatabaseUser): F[Boolean] =
    singleUpdate {
      sql"""
      insert into users (id, created_at, email, password, first_name, last_name, deleted)
        values (
          ${databaseUser.id},
          ${databaseUser.createdAt},
          ${databaseUser.email},
          ${databaseUser.password},
          ${databaseUser.firstName},
          ${databaseUser.lastName},
          false
        )
      """.update.run
        .transact(transactor)
    }

  override def findById(id: UUID): OptionT[F, DatabaseUser] =
    OptionT {
      sql"select id, created_at, email, password, first_name, last_name from users where id = $id and deleted = false"
        .query[DatabaseUser]
        .option
        .transact(transactor)
    }

  override def findByEmail(email: EmailAddress): OptionT[F, DatabaseUser] =
    OptionT {
      sql"select id, created_at, email, password, first_name, last_name from users where email = $email and deleted = false"
        .query[DatabaseUser]
        .option
        .transact(transactor)
    }

  override def updatePassword(id: UUID, password: String): F[Boolean] =
    singleUpdate {
      sql"update users set password = $password where id = $id and deleted = false".update.run
        .transact(transactor)
    }

  override def deleteById(userId: UUID): F[Boolean] =
    singleUpdate {
      sql"update users set deleted = true where id = $userId and deleted = false".update.run.transact(transactor)
    }
}
