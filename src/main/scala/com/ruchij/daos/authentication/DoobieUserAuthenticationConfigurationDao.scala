package com.ruchij.daos.authentication

import java.util.UUID
import java.util.concurrent.TimeUnit

import cats.data.OptionT
import cats.effect.{Bracket, Clock}
import cats.implicits._
import com.ruchij.daos.authentication.models.UserAuthenticationConfiguration
import com.ruchij.daos.doobie.singleUpdate
import com.ruchij.daos.doobie.DoobieCustomMappings._
import doobie.postgres.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.joda.time.DateTime

import scala.language.higherKinds

class DoobieUserAuthenticationConfigurationDao[F[_]: Bracket[*[_], Throwable]: Clock](transactor: Transactor.Aux[F, Unit])
    extends UserAuthenticationConfigurationDao[F] {

  override def insert(authenticationConfiguration: UserAuthenticationConfiguration): F[Boolean] =
    singleUpdate {
      sql"""
      INSERT INTO authentication_configuration (user_id, created_at, last_modified_at, password, totp_secret)
        VALUES (
          ${authenticationConfiguration.userId},
          ${authenticationConfiguration.createdAt},
          ${authenticationConfiguration.lastModifiedAt},
          ${authenticationConfiguration.password},
          ${authenticationConfiguration.totpSecret}
        )
     """
        .update
        .run
        .transact(transactor)
    }

  override def findByUserId(userId: UUID): OptionT[F, UserAuthenticationConfiguration] =
    OptionT {
      sql"""
        SELECT user_id, created_at, last_modified_at, password, totp_secret
          FROM authentication_configuration WHERE user_id = $userId
      """
        .query[UserAuthenticationConfiguration]
        .option
        .transact(transactor)
    }

  override def updatePassword(userId: UUID, password: String): F[Boolean] =
    Clock[F].realTime(TimeUnit.MILLISECONDS)
      .flatMap { timestamp =>
        singleUpdate {
          sql"""
            UPDATE authentication_configuration SET password = $password, last_modified_at = ${new DateTime(timestamp)}
              WHERE user_id = $userId
          """
            .update
            .run
            .transact(transactor)
        }
      }

  override def updateTotpSecret(userId: UUID, totpSecret: Option[String]): F[Boolean] =
    Clock[F].realTime(TimeUnit.MILLISECONDS)
      .flatMap { timestamp =>
        singleUpdate {
          sql"""
            UPDATE authentication_configuration SET totp_secret = $totpSecret, last_modified_at = ${new DateTime(timestamp)}
              WHERE user_id = $userId
          """
            .update
            .run
            .transact(transactor)
        }
      }
}
