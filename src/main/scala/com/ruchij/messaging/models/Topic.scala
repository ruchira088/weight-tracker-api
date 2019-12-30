package com.ruchij.messaging.models

import com.ruchij.avro4s.Decoders._
import com.ruchij.avro4s.Encoders._
import com.ruchij.avro4s.Schemas._
import com.ruchij.services.user.models.User
import com.sksamuel.avro4s.RecordFormat
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq
import scala.language.higherKinds

sealed trait Topic[A] extends EnumEntry {
  val recordFormat: RecordFormat[A]
}

object Topic extends Enum[Topic[_]] {

  case object UserCreated extends Topic[User] {
    override val recordFormat: RecordFormat[User] = RecordFormat[User]
  }

  override def values: IndexedSeq[Topic[_]] = findValues
}
