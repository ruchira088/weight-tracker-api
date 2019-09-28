package com.ruchij.daos.user

import java.util.UUID

import cats.data.OptionT
import com.ruchij.daos.user.models.DatabaseUser

import scala.language.higherKinds

trait UserDao[F[_]] {
  def insert(databaseUser: DatabaseUser): F[Int]

  def findById(id: UUID): OptionT[F, DatabaseUser]

  def findByUsername(username: String): OptionT[F, DatabaseUser]

  def findByEmail(email: String): OptionT[F, DatabaseUser]
}
