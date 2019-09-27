package com.ruchij.services.authentication

import com.ruchij.models.User

import scala.language.higherKinds

trait AuthenticationSecretGenerator[F[_]] {
  def generate(user: User): F[String]
}
