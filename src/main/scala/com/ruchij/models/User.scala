package com.ruchij.models

import java.util.UUID

import com.ruchij.daos.user.models.DatabaseUser

case class User(id: UUID, username: String, email: String, firstName: Option[String], lastName: Option[String])

object User {
  def fromDatabaseUser(databaseUser: DatabaseUser): User = ???
}
