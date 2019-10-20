package com.ruchij.web.responses

import cats.Applicative
import com.ruchij.circe.Encoders.jodaTimeEncoder
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import org.joda.time.DateTime

import scala.language.higherKinds

case class ResetPasswordResponse(email: String, expiresAt: DateTime)

object ResetPasswordResponse {
  implicit def resetPasswordResponseEncoder[F[_]: Applicative]: EntityEncoder[F, ResetPasswordResponse] =
    jsonEncoderOf[F, ResetPasswordResponse]
}
