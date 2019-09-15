package com.ruchij.services.hashing

import cats.effect.{Blocker, ContextShift, IO, Sync}
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

class BCryptService[F[_]: ContextShift: Sync](cpuBlockingExecutionContext: ExecutionContext)
    extends PasswordHashingService[F, String] {

  override def hash(password: String): F[String] =
    Blocker
      .liftExecutionContext(cpuBlockingExecutionContext)
      .delay {
        BCrypt.hashpw(password, BCrypt.gensalt())
      }

  override def checkPassword(password: String, hashedValue: String): F[Boolean] =
    Blocker
      .liftExecutionContext(cpuBlockingExecutionContext)
      .delay {
        BCrypt.checkpw(password, hashedValue)
      }
}
