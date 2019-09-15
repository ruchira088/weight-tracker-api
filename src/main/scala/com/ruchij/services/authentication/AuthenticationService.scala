package com.ruchij.services.authentication

import com.ruchij.models.User

import scala.language.higherKinds

trait AuthenticationService[F[_]] {
  def hashPassword(password: String): F[String]

  def login(username: String, password: String): F[User]
}
