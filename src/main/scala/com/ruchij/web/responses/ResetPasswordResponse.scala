package com.ruchij.web.responses

import cats.Applicative
import com.ruchij.circe.Encoders.{jodaTimeCirceEncoder, taggedStringCirceEncoder}
import com.ruchij.types.Tags.EmailAddress
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
