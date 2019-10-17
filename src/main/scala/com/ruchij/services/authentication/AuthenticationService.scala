package com.ruchij.services.authentication

import com.ruchij.services.user.models.User
import com.ruchij.services.authentication.models.AuthenticationToken

import scala.language.higherKinds

trait AuthenticationService[F[_]] {
  def hashPassword(password: String): F[String]

  def login(email: String, password: String): F[AuthenticationToken]

  def logout(secret: String): F[AuthenticationToken]

  def authenticate(secret: String): F[User]
}
