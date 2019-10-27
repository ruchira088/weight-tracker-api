package com.ruchij.daos.user

import java.util.UUID

import cats.data.OptionT
import com.ruchij.daos.user.models.DatabaseUser
import com.ruchij.services.email.models.Email.EmailAddress

import scala.language.higherKinds

trait UserDao[F[_]] {
  def insert(databaseUser: DatabaseUser): F[Boolean]

  def findById(userId: UUID): OptionT[F, DatabaseUser]

  def findByEmail(email: EmailAddress): OptionT[F, DatabaseUser]

  def updatePassword(userId: UUID, hashedPassword: String): F[Boolean]
}
