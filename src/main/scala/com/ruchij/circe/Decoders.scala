package com.ruchij.circe

import java.nio.file.{Path, Paths}

import io.circe.Decoder
import org.http4s.MediaType
import org.joda.time.DateTime
import shapeless.tag
import shapeless.tag.@@

import scala.util.Try

object Decoders {
  implicit val jodaTimeCirceDecoder: Decoder[DateTime] =
    Decoder.decodeString.emapTry(string => Try(DateTime.parse(string)))

  implicit val optionStringCirceDecoder: Decoder[Option[String]] =
    Decoder.decodeOption[String].emap(value => Right(value.filter(_.trim.nonEmpty)))

  implicit val mediaTypeCirceDecoder: Decoder[MediaType] =
    Decoder.decodeString.emap(string => MediaType.parse(string).left.map(_.message))

  implicit val pathCirceDecoder: Decoder[Path] = Decoder.decodeString.map(string => Paths.get(string))

  implicit def taggedStringCirceDecoder[A]: Decoder[String @@ A] = Decoder.decodeString.map(tag[A][String])
}
