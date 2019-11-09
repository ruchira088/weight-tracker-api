package com.ruchij.circe

import io.circe.Encoder
import org.joda.time.DateTime
import shapeless.tag.@@

object Encoders {
  implicit val jodaTimeEncoder: Encoder[DateTime] = Encoder.encodeString.contramap[DateTime](_.toString)

  implicit def throwableEncoder[A <: Throwable]: Encoder[A] = Encoder.encodeString.contramap(_.getMessage)

  implicit def taggedStringEncoder[A]: Encoder[String @@ A] = Encoder.encodeString.contramap(_.toString)
}
