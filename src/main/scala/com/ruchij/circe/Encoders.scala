package com.ruchij.circe

import io.circe.Encoder
import org.joda.time.DateTime

object Encoders {
  implicit val jodaTimeEncoder: Encoder[DateTime] = Encoder.encodeString.contramap[DateTime](_.toString)
}
