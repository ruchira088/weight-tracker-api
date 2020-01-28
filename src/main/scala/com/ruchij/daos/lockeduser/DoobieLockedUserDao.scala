package com.ruchij.daos.lockeduser

import java.util.UUID

import cats.data.OptionT
import cats.effect.{Bracket, Clock}
import cats.implicits._
import com.ruchij.daos.lockeduser.models.DatabaseLockedUser
import com.ruchij.daos.doobie.singleUpdate
import com.ruchij.daos.doobie.DoobieCustomMappings.{dateTimeGet, dateTimePut}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import org.joda.time.DateTime

import scala.concurrent.duration.MILLISECONDS
import scala.language.higherKinds

class DoobieLockedUserDao[F[_]: Bracket[*[_], Throwable]: Clock](transactor: Transactor.Aux[F, Unit])
    extends LockedUserDao[F] {

  override def insert(databaseLockedUser: DatabaseLockedUser): F[Boolean] =
    singleUpdate {
      sql"""
        INSERT INTO locked_users (user_id, locked_at, unlock_code)
          VALUES (${databaseLockedUser.userId}, ${databaseLockedUser.lockedAt}, ${databaseLockedUser.unlockCode})
      """.update.run.transact(transactor)
    }

  override def findLockedUserById(userId: UUID): OptionT[F, DatabaseLockedUser] =
    OptionT {
      sql"""
        SELECT user_id, locked_at, unlock_code, unlocked_at FROM locked_users
          WHERE user_id = $userId AND unlocked_at IS NULL
      """
        .query[DatabaseLockedUser]
        .option
        .transact(transactor)
    }

  override def unlockUser(userId: UUID, unlockCode: String): F[Boolean] =
    Clock[F]
      .realTime(MILLISECONDS)
      .flatMap { timestamp =>
        singleUpdate {
          sql"""
            UPDATE locked_users SET unlocked_at = ${new DateTime(timestamp)} WHERE user_id = $userId AND
              unlock_code = $unlockCode AND unlocked_at IS NULL
          """.update.run.transact(transactor)
        }
      }
}
