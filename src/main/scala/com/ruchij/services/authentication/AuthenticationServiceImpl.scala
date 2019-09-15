package com.ruchij.services.authentication

import cats.effect.Sync
import com.ruchij.models.User
import com.ruchij.services.hashing.PasswordHashingService

import scala.language.higherKinds

class AuthenticationServiceImpl[F[_]: Sync](passwordHashingService: PasswordHashingService[F, String])
    extends AuthenticationService[F] {

  override def hashPassword(password: String): F[String] = passwordHashingService.hash(password)

  override def login(username: String, password: String): F[User] = ???
}
