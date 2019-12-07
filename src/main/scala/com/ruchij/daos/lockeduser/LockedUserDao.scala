package com.ruchij.daos.lockeduser

import java.util.UUID

import com.ruchij.daos.lockeduser.models.DatabaseLockedUser

import scala.language.higherKinds

trait LockedUserDao[F[_]] {
  def insert(databaseLockedUser: DatabaseLockedUser): F[Boolean]

  def findLockedUserById(userId: UUID): F[Option[DatabaseLockedUser]]
}
