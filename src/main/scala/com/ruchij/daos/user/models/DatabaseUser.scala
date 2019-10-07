package com.ruchij.daos.user.models

import java.util.UUID

import org.joda.time.DateTime

case class DatabaseUser(
  id: UUID,
  createdAt: DateTime,
  email: String,
  password: String,
  firstName: String,
  lastName: Option[String]
)
