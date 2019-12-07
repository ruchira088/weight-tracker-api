package com.ruchij.daos.resetpassword

import java.util.UUID

import cats.data.OptionT
import cats.effect.Sync
import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import com.ruchij.daos.doobie.singleUpdate
import com.ruchij.daos.doobie.DoobieCustomMappings._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import doobie.implicits._
import org.joda.time.DateTime

import scala.language.higherKinds

class DoobieResetPasswordTokenDao[F[_]: Sync](transactor: Transactor.Aux[F, Unit]) extends ResetPasswordTokenDao[F] {

  override def insert(databaseResetPasswordToken: DatabaseResetPasswordToken): F[Boolean] =
    singleUpdate {
      sql"""
        insert into reset_password_tokens (secret, user_id, created_at, expires_at, password_set_at)
        values (
          ${databaseResetPasswordToken.secret},
          ${databaseResetPasswordToken.userId},
          ${databaseResetPasswordToken.createdAt},
          ${databaseResetPasswordToken.expiresAt},
          ${databaseResetPasswordToken.passwordSetAt}
        )
        """
        .update.run.transact(transactor)
    }

  override def find(userId: UUID, secret: String): OptionT[F, DatabaseResetPasswordToken] =
    OptionT {
      sql"""
        select user_id, secret, created_at, expires_at, password_set_at from reset_password_tokens
          where user_id = $userId and secret = $secret
      """
        .query[DatabaseResetPasswordToken]
        .option
        .transact(transactor)
    }

  override def resetCompleted(userId: UUID, secret: String, timestamp: DateTime): F[Boolean] =
    singleUpdate {
      sql"""
        update reset_password_tokens set password_set_at = $timestamp
          where user_id = $userId and secret = $secret
        """.update.run.transact(transactor)
    }
}
