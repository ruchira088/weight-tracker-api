package com.ruchij.web.circe

import cats.Applicative
import io.circe.{Encoder, Json}
import org.joda.time.DateTime

import scala.language.higherKinds

object Encoders {
  implicit def jodaTimeDecoder[F[_]: Applicative]: Encoder[DateTime] =
    (dateTime: DateTime) => Json.fromString(dateTime.toString)
}
