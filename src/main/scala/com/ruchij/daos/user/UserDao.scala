package com.ruchij.daos.user

import java.util.UUID

import cats.data.OptionT
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.types.Tags.EmailAddress

import scala.language.higherKinds

trait UserDao[F[_]] {
  def insert(databaseUser: DatabaseUser): F[Boolean]

  def findById(userId: UUID): OptionT[F, DatabaseUser]

  def findByEmail(email: EmailAddress): OptionT[F, DatabaseUser]

  def deleteById(userId: UUID): F[Boolean]

  def updatePassword(userId: UUID, hashedPassword: String): F[Boolean]

  def updateProfileImage(userId: UUID, imageKey: String): F[Boolean]
}
