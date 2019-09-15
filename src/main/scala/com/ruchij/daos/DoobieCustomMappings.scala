package com.ruchij.daos

import java.sql.Timestamp

import doobie.util.{Get, Put}
import org.joda.time.DateTime

object DoobieCustomMappings {

  implicit val dateTimeGet: Get[DateTime] = Get[Timestamp].tmap(timestamp => new DateTime(timestamp.getTime))

  implicit val dateTimePut: Put[DateTime] = Put[Timestamp].tcontramap(dateTime => new Timestamp(dateTime.getMillis))
}
