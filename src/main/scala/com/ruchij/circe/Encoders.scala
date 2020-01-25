package com.ruchij.circe

import java.nio.file.Path

import cats.Show
import io.circe.Encoder
import org.http4s.MediaType
import org.joda.time.DateTime
import shapeless.tag.@@

object Encoders {
  implicit val jodaTimeCirceEncoder: Encoder[DateTime] = Encoder.encodeString.contramap[DateTime](_.toString)

  implicit val mediaTypeCirceEncoder: Encoder[MediaType] =
    Encoder.encodeString.contramap[MediaType](Show[MediaType].show)

  implicit val pathCirceEncoder: Encoder[Path] = Encoder.encodeString.contramap[Path](_.toString)

  implicit def throwableCirceEncoder[A <: Throwable]: Encoder[A] = Encoder.encodeString.contramap(_.getMessage)

  implicit def taggedStringCirceEncoder[A]: Encoder[String @@ A] = Encoder.encodeString.contramap(_.toString)
}
