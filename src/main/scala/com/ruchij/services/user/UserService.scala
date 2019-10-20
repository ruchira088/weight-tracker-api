package com.ruchij.services.user

import java.util.UUID

import cats.data.OptionT
import com.ruchij.services.user.models.User

import scala.language.higherKinds

trait UserService[F[_]] {
  def create(email: String, password: String, firstName: String, lastName: Option[String]): F[User]

  def getById(id: UUID): F[User]

  def findByEmail(email: String): OptionT[F, User]

  def updatePassword(userId: UUID, secret: String, password: String): F[User]
}
