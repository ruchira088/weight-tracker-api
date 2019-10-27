package com.ruchij.circe

import io.circe.Decoder
import org.joda.time.DateTime
import shapeless.tag
import shapeless.tag.@@

import scala.util.Try

object Decoders {
  implicit val jodaTimeDecoder: Decoder[DateTime] =
    Decoder.decodeString.emapTry(string => Try(DateTime.parse(string)))

  implicit val optionStringDecoder: Decoder[Option[String]] =
    Decoder.decodeOption[String].emap(value => Right(value.filter(_.trim.nonEmpty)))

  implicit def taggedStringDecoder[A]: Decoder[String @@ A] = Decoder.decodeString.map(tag[A][String])
}
