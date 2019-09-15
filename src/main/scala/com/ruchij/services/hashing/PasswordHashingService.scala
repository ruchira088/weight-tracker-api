package com.ruchij.services.hashing

import scala.language.higherKinds

trait PasswordHashingService[F[_], A] {
  def hash(password: String): F[A]

  def checkPassword(password: String, hashedValue: A): F[Boolean]
}
