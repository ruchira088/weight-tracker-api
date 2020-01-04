package com.ruchij.messaging.models

import com.ruchij.avro4s.Decoders._
import com.ruchij.avro4s.Encoders._
import com.ruchij.avro4s.Schemas._
import com.ruchij.circe.CirceEnum
import com.ruchij.circe.Decoders._
import com.ruchij.circe.Encoders._
import com.ruchij.services.user.models.User
import com.sksamuel.avro4s.RecordFormat
import enumeratum.{Enum, EnumEntry}
import io.circe.{Codec, Encoder}
import io.circe.generic.semiauto.deriveCodec

import scala.collection.immutable.IndexedSeq
import scala.language.higherKinds

sealed trait Topic[A] extends EnumEntry {
  val recordFormat: RecordFormat[A]
  val codec: Codec[A]
}

object Topic extends Enum[Topic[_]] with CirceEnum[Topic[_]] {

  implicit case object UserCreated extends Topic[User] {
    override val recordFormat: RecordFormat[User] = RecordFormat[User]
    override val codec: Codec[User] = deriveCodec[User]
  }

  override def values: IndexedSeq[Topic[_]] = findValues

  implicit def topicEncoder[A]: Encoder[Topic[A]] = enumEncoder.contramap[Topic[A]](identity)
}
