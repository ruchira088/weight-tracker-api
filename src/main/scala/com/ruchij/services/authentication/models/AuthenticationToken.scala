package com.ruchij.services.authentication.models

import java.util.UUID

import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import org.joda.time.DateTime

case class AuthenticationToken(userId: UUID, expiresAt: DateTime, secret: UUID)

object AuthenticationToken {
  def fromDatabaseAuthenticationToken(databaseAuthenticationToken: DatabaseAuthenticationToken): AuthenticationToken =
    AuthenticationToken(
      databaseAuthenticationToken.userId,
      databaseAuthenticationToken.expiresAt,
      databaseAuthenticationToken.secret
    )
}
