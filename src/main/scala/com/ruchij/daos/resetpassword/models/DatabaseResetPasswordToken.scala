package com.ruchij.daos.resetpassword.models

import java.util.UUID

import org.joda.time.DateTime

case class DatabaseResetPasswordToken(
  userId: UUID,
  secret: String,
  createdAt: DateTime,
  expiresAt: DateTime,
  passwordSetAt: Option[DateTime]
)
