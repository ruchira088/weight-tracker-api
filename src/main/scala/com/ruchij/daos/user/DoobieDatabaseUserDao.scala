package com.ruchij.daos.user

import java.util.UUID

import cats.data.OptionT
import cats.effect.{Async, ContextShift, Sync}
import cats.implicits.toFlatMapOps
import com.ruchij.config.DoobieConfiguration
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.exceptions.DatabaseException
import doobie.postgres.implicits._
import com.ruchij.daos.DoobieCustomMappings._
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.language.higherKinds

class DoobieDatabaseUserDao[F[_]: Sync](transactor: Transactor.Aux[F, Unit]) extends DatabaseUserDao[F] {
  override def insert(databaseUser: DatabaseUser): F[DatabaseUser] =
    sql"""
      insert into users (id, created_at, username, password, email, first_name, last_name) values
      (${databaseUser.id}, ${databaseUser.createdAt}, ${databaseUser.username}, ${databaseUser.password}, ${databaseUser.email}, ${databaseUser.firstName}, ${databaseUser.lastName})
      """.update.run
      .transact(transactor)
      .flatMap { _ =>
        findById(databaseUser.id)
          .getOrElseF(Sync[F].raiseError(DatabaseException("Unable to persist user")))
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

object DoobieDatabaseUserDao {
  def fromConfiguration[M[_]: Async: ContextShift](doobieConfiguration: DoobieConfiguration): DoobieDatabaseUserDao[M] =
    new DoobieDatabaseUserDao(
      Transactor.fromDriverManager[M](
        doobieConfiguration.driver,
        doobieConfiguration.url,
        doobieConfiguration.user,
        doobieConfiguration.password
      )
    )
}
