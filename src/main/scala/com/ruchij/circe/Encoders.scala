package com.ruchij.circe

import io.circe.Encoder
import org.joda.time.DateTime

import scala.language.higherKinds

object Encoders {
  implicit val jodaTimeEncoder: Encoder[DateTime] = Encoder.encodeString.contramap[DateTime](_.toString)
}
