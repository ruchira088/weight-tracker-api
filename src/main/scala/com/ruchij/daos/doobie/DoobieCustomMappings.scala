package com.ruchij.daos.doobie

import java.sql.Timestamp

import doobie.util.{Get, Put}
import org.joda.time.DateTime
import shapeless.tag.@@
import shapeless.tag

import scala.reflect.runtime.universe.TypeTag

object DoobieCustomMappings {

  implicit val dateTimeGet: Get[DateTime] = Get[Timestamp].tmap(timestamp => new DateTime(timestamp.getTime))

  implicit val dateTimePut: Put[DateTime] = Put[Timestamp].tcontramap(dateTime => new Timestamp(dateTime.getMillis))

  implicit def taggedStringGet[A: TypeTag]: Get[String @@ A] = Get[String].tmap(tag[A][String])

  implicit def taggedStringPut[A: TypeTag]: Put[String @@ A] = Put[String].tcontramap(_.toString)
}
