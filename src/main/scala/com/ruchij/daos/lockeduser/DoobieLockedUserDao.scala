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
        insert into locked_users (user_id, locked_at, unlock_code)
          values (${databaseLockedUser.userId}, ${databaseLockedUser.lockedAt}, ${databaseLockedUser.unlockCode})
      """.update.run.transact(transactor)
    }

  override def findLockedUserById(userId: UUID): OptionT[F, DatabaseLockedUser] =
    OptionT {
      sql"select user_id, locked_at, unlock_code, unlocked_at from locked_users where user_id = $userId and unlocked_at is null"
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
            update locked_users set unlocked_at = ${new DateTime(timestamp)} where user_id = $userId and 
              unlock_code = $unlockCode and unlocked_at is null
          """.update.run.transact(transactor)
        }
      }

}
