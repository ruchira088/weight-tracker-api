package com.ruchij.services.user

import cats.data.OptionT
import cats.effect.Clock
import com.ruchij.models.User

import scala.language.higherKinds

trait UserService[F[_]] {
  def create(username: String, password: String, email: String, firstName: Option[String], lastName: Option[String])(
    implicit clock: Clock[F]
  ): F[User]

  def findByUsername(username: String): OptionT[F, User]

  def findByEmail(email: String): OptionT[F, User]
}
