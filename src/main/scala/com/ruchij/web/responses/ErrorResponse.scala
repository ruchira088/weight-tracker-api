package com.ruchij.web.responses

import cats.Applicative
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

import scala.language.higherKinds

case class ErrorResponse(errorMessages: List[String])

object ErrorResponse {
  def apply(throwable: Throwable): ErrorResponse =
    ErrorResponse(List(throwable.getMessage))

  implicit def errorResponseEncoder[F[_]: Applicative]: EntityEncoder[F, ErrorResponse] =
    jsonEncoderOf[F, ErrorResponse]
}
