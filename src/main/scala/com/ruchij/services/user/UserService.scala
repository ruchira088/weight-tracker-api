package com.ruchij.services.user

import java.util.UUID

import cats.data.OptionT
import com.ruchij.services.email.models.Email.EmailAddress
import com.ruchij.services.user.models.User

import scala.language.higherKinds

trait UserService[F[_]] {
  def create(email: EmailAddress, password: String, firstName: String, lastName: Option[String]): F[User]

  def getById(id: UUID): F[User]

  def findByEmail(email: EmailAddress): OptionT[F, User]

  def updatePassword(userId: UUID, secret: String, password: String): F[User]

  def unlockUser(userId: UUID, unlockCode: String): F[User]
}
