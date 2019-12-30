package com.ruchij.avro4s

import com.sksamuel.avro4s.SchemaFor
import com.sksamuel.avro4s.SchemaFor.StringSchemaFor
import shapeless.tag.@@

object Schemas {
  implicit def taggedStringSchema[A]: SchemaFor[String @@ A] = StringSchemaFor.map[String @@ A](identity)
}
