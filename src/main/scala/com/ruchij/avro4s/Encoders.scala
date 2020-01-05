package com.ruchij.avro4s

import java.sql.Timestamp

import com.sksamuel.avro4s.Encoder
import com.sksamuel.avro4s.Encoder.{StringEncoder, TimestampEncoder}
import org.joda.time.DateTime
import shapeless.tag.@@

object Encoders {
  implicit def taggedStringAvro4sEncoder[A]: Encoder[String @@ A] = StringEncoder.comap(_.toString)

  implicit val jodaDateTimeAvro4sEncoder: Encoder[DateTime] =
    TimestampEncoder.comap[DateTime](dateTime => new Timestamp(dateTime.getMillis))
}
