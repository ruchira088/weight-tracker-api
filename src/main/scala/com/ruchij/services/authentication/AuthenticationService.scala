package com.ruchij.services.authentication

import java.util.UUID

import com.ruchij.services.user.models.User
import com.ruchij.services.authentication.models.{AuthenticationToken, ResetPasswordToken}

import scala.language.higherKinds

trait AuthenticationService[F[_]] {
  def hashPassword(password: String): F[String]

  def login(email: String, password: String): F[AuthenticationToken]

  def logout(secret: String): F[AuthenticationToken]

  def authenticate(secret: String): F[User]

  def resetPassword(email: String): F[ResetPasswordToken]

  def getResetPasswordToken(userId: UUID, secret: String): F[ResetPasswordToken]

  def passwordResetCompleted(userId: UUID, secret: String): F[ResetPasswordToken]
}
