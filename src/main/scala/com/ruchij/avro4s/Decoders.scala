package com.ruchij.avro4s

import com.sksamuel.avro4s.Decoder
import com.sksamuel.avro4s.Decoder.StringDecoder
import shapeless.tag
import shapeless.tag.@@

object Decoders {
  implicit def taggedStringAvro4sDecoder[A]: Decoder[String @@ A] = StringDecoder.map(tag[A][String])
}
