package com.ruchij.daos.authenticationfailure

import java.util.UUID

import cats.effect.Bracket
import com.ruchij.daos.authenticationfailure.models.DatabaseAuthenticationFailure
import com.ruchij.daos.doobie.singleUpdate
import com.ruchij.daos.doobie.DoobieCustomMappings.{dateTimeGet, dateTimePut}
import doobie.postgres.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.joda.time.DateTime

import scala.language.higherKinds

class DoobieAuthenticationFailureDao[F[_]: Bracket[*[_], Throwable]](transactor: Transactor.Aux[F, Unit])
    extends AuthenticationFailureDao[F] {

  override def insert(databaseAuthenticationFailure: DatabaseAuthenticationFailure): F[Boolean] =
    singleUpdate {
      sql"""
        INSERT INTO authentication_failures (id, user_id, failed_at, deleted)
          VALUES (
            ${databaseAuthenticationFailure.id},
            ${databaseAuthenticationFailure.userId},
            ${databaseAuthenticationFailure.failedAt},
            ${databaseAuthenticationFailure.deleted}
          )
      """.update.run.transact(transactor)
    }

  override def findByUser(userId: UUID, after: DateTime): F[List[DatabaseAuthenticationFailure]] =
    sql"""
      SELECT id, user_id, failed_at, deleted FROM authentication_failures WHERE user_id = $userId
        AND failed_at > $after AND deleted = false
    """
      .query[DatabaseAuthenticationFailure]
      .to[List]
      .transact(transactor)

  override def delete(id: UUID): F[Boolean] =
    singleUpdate {
      sql"UPDATE authentication_failures SET deleted = true WHERE id = $id".update.run.transact(transactor)
    }
}
