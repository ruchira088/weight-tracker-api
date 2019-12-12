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
        insert into authentication_failures (id, user_id, failed_at, deleted)
          values (
            ${databaseAuthenticationFailure.id},
            ${databaseAuthenticationFailure.userId},
            ${databaseAuthenticationFailure.failedAt},
            ${databaseAuthenticationFailure.deleted}
          )
      """.update.run.transact(transactor)
    }

  override def findByUser(userId: UUID, after: DateTime): F[List[DatabaseAuthenticationFailure]] =
    sql"""
      select id, user_id, failed_at, deleted from authentication_failures where user_id = $userId
        and failed_at > $after and deleted = false
    """
      .query[DatabaseAuthenticationFailure]
      .to[List]
      .transact(transactor)

  override def delete(id: UUID): F[Boolean] =
    singleUpdate {
      sql"update authentication_failures set deleted = true where id = $id".update.run.transact(transactor)
    }
}
