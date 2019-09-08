package com.ruchij.daos.user

import cats.data.OptionT
import com.ruchij.daos.user.models.DatabaseUser

import scala.language.higherKinds

trait DatabaseUserDao[F[_]] {
  def insert(databaseUser: DatabaseUser): F[DatabaseUser]

  def findByUsername(username: String): OptionT[F, DatabaseUser]

  def findByEmail(username: String): OptionT[F, DatabaseUser]
}
