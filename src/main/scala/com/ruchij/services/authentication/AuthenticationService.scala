package com.ruchij.services.authentication

import java.util.UUID

import com.ruchij.models.User
import com.ruchij.services.authentication.models.AuthenticationToken

import scala.language.higherKinds

trait AuthenticationService[F[_]] {
  def hashPassword(password: String): F[String]

  def login(username: String, password: String): F[AuthenticationToken]

  def authenticate(userId: UUID, authenticationSecret: UUID): F[User]
}
