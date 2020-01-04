package com.ruchij.circe

import enumeratum.{Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

trait CirceEnum[A <: EnumEntry] { enums: Enum[A] =>
  implicit val enumEncoder: Encoder[A] = Encoder.encodeString.contramap[A](_.entryName)

  implicit val enumDecoder: Decoder[A] =
    Decoder.decodeString.emap { string => withNameInsensitiveEither(string).left.map(_.getMessage()) }
}
