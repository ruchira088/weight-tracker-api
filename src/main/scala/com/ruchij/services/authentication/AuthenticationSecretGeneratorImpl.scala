package com.ruchij.services.authentication

import cats.Applicative
import cats.implicits._
import com.ruchij.services.user.models.User
import com.ruchij.types.RandomUuid

import scala.language.higherKinds

class AuthenticationSecretGeneratorImpl[F[_]: RandomUuid: Applicative] extends AuthenticationSecretGenerator[F] {
  override def generate(user: User): F[String] = RandomUuid[F].uuid.map(_.toString)
}
