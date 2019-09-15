package com.ruchij.daos.user.models

import java.util.UUID

import org.joda.time.DateTime

case class DatabaseUser(
  id: UUID,
  createdAt: DateTime,
  username: String,
  password: String,
  email: String,
  firstName: Option[String],
  lastName: Option[String]
)
