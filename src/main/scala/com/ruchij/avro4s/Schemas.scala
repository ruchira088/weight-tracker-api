package com.ruchij.avro4s

import com.sksamuel.avro4s.SchemaFor
import com.sksamuel.avro4s.SchemaFor.{StringSchemaFor, TimestampSchemaFor}
import org.joda.time.DateTime
import shapeless.tag.@@

object Schemas {
  implicit def taggedStringAvro4sSchema[A]: SchemaFor[String @@ A] = StringSchemaFor.map[String @@ A](identity)

  implicit val jodaDateTimeAvro4sSchema: SchemaFor[DateTime] = TimestampSchemaFor.map[DateTime](identity)
}
