package com.ruchij.web.responses

import cats.Applicative
import io.circe.Encoder
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

import scala.language.higherKinds

case class ErrorResponse(errorMessages: List[Throwable])

object ErrorResponse {
  implicit def throwableEncoder[F[_]: Applicative]: Encoder[Throwable] =
    Encoder.encodeString.contramap(_.getMessage)

  implicit def errorResponseEncoder[F[_]: Applicative]: EntityEncoder[F, ErrorResponse] =
    jsonEncoderOf[F, ErrorResponse]
}
