package com.ruchij.daos.user

import java.util.UUID
import java.util.concurrent.TimeUnit

import cats.data.OptionT
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.ruchij.daos.user.models.DatabaseUser
import doobie.postgres.implicits._
import com.ruchij.daos.doobie.DoobieCustomMappings._
import com.ruchij.daos.doobie.singleUpdate
import com.ruchij.types.Tags.EmailAddress
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.joda.time.DateTime

import scala.language.higherKinds

class DoobieUserDao[F[_]: Sync: Clock](transactor: Transactor.Aux[F, Unit]) extends UserDao[F] {

  override def insert(databaseUser: DatabaseUser): F[Boolean] =
    singleUpdate {
      sql"""
        INSERT INTO users (id, created_at, last_modified_at, email, first_name, last_name, profile_image, deleted)
          VALUES (
            ${databaseUser.id},
            ${databaseUser.createdAt},
            ${databaseUser.lastModifiedAt},
            ${databaseUser.email},
            ${databaseUser.firstName},
            ${databaseUser.lastName},
            ${databaseUser.profileImage},
            false
        )
      """.update.run
        .transact(transactor)
    }

  override def findById(id: UUID): OptionT[F, DatabaseUser] =
    OptionT {
      sql"SELECT id, created_at, last_modified_at, email, first_name, last_name, profile_image FROM users WHERE id = $id AND deleted = false"
        .query[DatabaseUser]
        .option
        .transact(transactor)
    }

  override def findByEmail(email: EmailAddress): OptionT[F, DatabaseUser] =
    OptionT {
      sql"SELECT id, created_at, last_modified_at, email, first_name, last_name, profile_image FROM users WHERE email = $email AND deleted = false"
        .query[DatabaseUser]
        .option
        .transact(transactor)
    }

  override def deleteById(userId: UUID): F[Boolean] =
    singleUpdate {
      sql"UPDATE users SET deleted = true WHERE id = $userId AND deleted = false".update.run.transact(transactor)
    }

  override def updateProfileImage(userId: UUID, imageKey: String): F[Boolean] =
    Clock[F].realTime(TimeUnit.MILLISECONDS)
      .flatMap {
        timestamp =>
          singleUpdate {
            sql"""
             UPDATE users SET profile_image = $imageKey, last_modified_at = ${new DateTime(timestamp)}
              WHERE id = $userId AND deleted = false
            """
              .update.run.transact(transactor)
          }
      }
}
