package com.ruchij.circe

import io.circe.Encoder
import org.joda.time.DateTime
import shapeless.tag.@@

object Encoders {
  implicit val jodaTimeCirceEncoder: Encoder[DateTime] = Encoder.encodeString.contramap[DateTime](_.toString)

  implicit def throwableCirceEncoder[A <: Throwable]: Encoder[A] = Encoder.encodeString.contramap(_.getMessage)

  implicit def taggedStringCirceEncoder[A]: Encoder[String @@ A] = Encoder.encodeString.contramap(_.toString)
}
