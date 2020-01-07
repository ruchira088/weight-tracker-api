package com.ruchij.services.hashing

import cats.effect.{Blocker, ContextShift, Sync}
import org.mindrot.jbcrypt.BCrypt

import scala.language.higherKinds

class BCryptService[F[_]: ContextShift: Sync](blocker: Blocker) extends PasswordHashingService[F, String] {

  override def hash(password: String): F[String] =
    blocker.delay {
      BCrypt.hashpw(password, BCrypt.gensalt())
    }

  override def checkPassword(password: String, hashedValue: String): F[Boolean] =
    blocker.delay {
      BCrypt.checkpw(password, hashedValue)
    }
}
