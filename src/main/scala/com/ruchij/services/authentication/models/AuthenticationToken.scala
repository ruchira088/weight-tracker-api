package com.ruchij.services.authentication.models

import java.util.UUID

import cats.Applicative
import com.ruchij.circe.Encoders.jodaTimeCirceEncoder
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import org.joda.time.DateTime

import scala.language.higherKinds

case class AuthenticationToken(userId: UUID, expiresAt: DateTime, secret: String)

object AuthenticationToken {
  def fromDatabaseAuthenticationToken(databaseAuthenticationToken: DatabaseAuthenticationToken): AuthenticationToken =
    AuthenticationToken(
      databaseAuthenticationToken.userId,
      databaseAuthenticationToken.expiresAt,
      databaseAuthenticationToken.secret
    )

  implicit def authenticationTokenEntityEncoder[F[_]: Applicative]: EntityEncoder[F, AuthenticationToken] =
    jsonEncoderOf[F, AuthenticationToken]
}
