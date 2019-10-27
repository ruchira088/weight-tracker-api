package com.ruchij.daos.user.models

import java.util.UUID

import com.ruchij.services.email.models.Email.EmailAddress
import org.joda.time.DateTime

case class DatabaseUser(
  id: UUID,
  createdAt: DateTime,
  email: EmailAddress,
  password: String,
  firstName: String,
  lastName: Option[String]
)
