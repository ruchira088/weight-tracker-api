package com.ruchij.services.user

import java.util.UUID

import cats.data.OptionT
import com.ruchij.services.user.models.User

import scala.language.higherKinds

trait UserService[F[_]] {
  def create(username: String, password: String, email: String, firstName: Option[String], lastName: Option[String]): F[User]

  def getById(id: UUID): F[User]

  def findByUsername(username: String): OptionT[F, User]

  def findByEmail(email: String): OptionT[F, User]
}
