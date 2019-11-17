package com.ruchij.services.authentication

import java.util.UUID

import cats.Applicative
import cats.implicits._
import com.ruchij.services.user.models.User
import com.ruchij.types.Random

import scala.language.higherKinds

class AuthenticationSecretGeneratorImpl[F[_]: Random[*[_], UUID]: Applicative] extends AuthenticationSecretGenerator[F] {
  override def generate(user: User): F[String] = Random[F, UUID].value.map(_.toString)
}
