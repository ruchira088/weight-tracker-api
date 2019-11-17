package com.ruchij.web.responses

import cats.Applicative
import com.ruchij.circe.Encoders.{jodaTimeEncoder, taggedStringEncoder}
import com.ruchij.services.email.models.Email.EmailAddress
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import org.joda.time.DateTime

import scala.language.higherKinds

case class ResetPasswordResponse(email: EmailAddress, expiresAt: DateTime, frontEndUrl: String)

object ResetPasswordResponse {
  implicit def resetPasswordResponseEncoder[F[_]: Applicative]: EntityEncoder[F, ResetPasswordResponse] =
    jsonEncoderOf[F, ResetPasswordResponse]
}
