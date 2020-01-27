package com.ruchij.services.authentication

import java.util.UUID

import com.ruchij.services.authentication.models.{AuthenticationToken, ResetPasswordToken}
import com.ruchij.services.user.models.User
import com.ruchij.types.Tags.EmailAddress

import scala.language.higherKinds

trait AuthenticationService[F[_]] {
  def login(email: EmailAddress, password: String): F[AuthenticationToken]

  def logout(secret: String): F[AuthenticationToken]

  def setPassword(userId: UUID, password: String): F[User]

  def authenticate(secret: String): F[User]

  def resetPassword(email: EmailAddress, frontEndUrl: String): F[ResetPasswordToken]

  def updatePassword(userId: UUID, secret: String, password: String): F[User]
}
