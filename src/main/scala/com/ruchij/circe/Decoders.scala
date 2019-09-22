package com.ruchij.circe

import io.circe.Decoder
import org.joda.time.DateTime

import scala.util.Try

object Decoders {
  implicit val jodaTimeDecoder: Decoder[DateTime] =
    Decoder.decodeString.emapTry(string => Try(DateTime.parse(string)))
}
