package com.ruchij.web.responses

import cats.Applicative
import com.ruchij.circe.Encoders.throwableCirceEncoder
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

import scala.language.higherKinds

case class ErrorResponse(errorMessages: List[Throwable])

object ErrorResponse {
  implicit def errorResponseEncoder[F[_]: Applicative]: EntityEncoder[F, ErrorResponse] =
    jsonEncoderOf[F, ErrorResponse]
}
