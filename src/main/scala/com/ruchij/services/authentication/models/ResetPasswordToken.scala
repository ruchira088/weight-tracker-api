package com.ruchij.services.authentication.models

import java.util.UUID

import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import org.joda.time.DateTime

case class ResetPasswordToken(userId: UUID, secret: String, expiresAt: DateTime, used: Boolean)

object ResetPasswordToken {
  def fromDatabaseResetPasswordToken(databaseResetPasswordToken: DatabaseResetPasswordToken): ResetPasswordToken =
    ResetPasswordToken(
      databaseResetPasswordToken.userId,
      databaseResetPasswordToken.secret,
      databaseResetPasswordToken.expiresAt,
      databaseResetPasswordToken.passwordSetAt.nonEmpty
    )
}
