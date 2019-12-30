package com.ruchij.avro4s

import com.sksamuel.avro4s.Encoder
import com.sksamuel.avro4s.Encoder.StringEncoder
import shapeless.tag.@@

object Encoders {
  implicit def taggedStringEncoder[A]: Encoder[String @@ A] = StringEncoder.comap(_.toString)
}
