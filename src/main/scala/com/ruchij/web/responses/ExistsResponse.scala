package com.ruchij.web.responses

import cats.Applicative
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

import scala.language.higherKinds

case class ExistsResponse(exists: Boolean)

object ExistsResponse {

  implicit def existsEncoder[F[_]: Applicative]: EntityEncoder[F, ExistsResponse] = jsonEncoderOf[F, ExistsResponse]
}
