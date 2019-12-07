package com.ruchij.daos.authenticationfailure

import java.util.UUID

import cats.effect.Bracket
import com.ruchij.daos.authenticationfailure.models.DatabaseAuthenticationFailure
import com.ruchij.daos.doobie.singleUpdate
import com.ruchij.daos.doobie.DoobieCustomMappings.{dateTimeGet, dateTimePut}
import doobie.postgres.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.language.higherKinds

class DoobieAuthenticationFailureDao[F[_]: Bracket[*[_], Throwable]](transactor: Transactor.Aux[F, Unit]) extends AuthenticationFailureDao[F] {

  override def insert(databaseAuthenticationFailure: DatabaseAuthenticationFailure): F[Boolean] =
    singleUpdate {
      sql"""
        insert into authentication_failure (user_id, failed_at)
          values (${databaseAuthenticationFailure.userId}, ${databaseAuthenticationFailure.failedAt})
      """.update.run.transact(transactor)
    }

  override def findByUser(userId: UUID): F[List[DatabaseAuthenticationFailure]] =
    sql"select user_id, failed_at from authentication_failure where user_id = $userId"
      .query[DatabaseAuthenticationFailure]
      .to[List]
      .transact(transactor)
}
