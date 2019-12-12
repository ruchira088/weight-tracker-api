package com.ruchij.daos.lockeduser

import java.util.UUID

import cats.data.OptionT
import cats.effect.Bracket
import com.ruchij.daos.lockeduser.models.DatabaseLockedUser
import com.ruchij.daos.doobie.singleUpdate
import com.ruchij.daos.doobie.DoobieCustomMappings.{dateTimeGet, dateTimePut}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor

import scala.language.higherKinds

class DoobieLockedUserDao[F[_]: Bracket[*[_], Throwable]](transactor: Transactor.Aux[F, Unit])
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
}
