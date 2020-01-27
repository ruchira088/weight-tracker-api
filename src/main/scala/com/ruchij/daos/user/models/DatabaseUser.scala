package com.ruchij.daos.user.models

import java.util.UUID

import com.ruchij.types.Tags.EmailAddress
import org.joda.time.DateTime

case class DatabaseUser(
  id: UUID,
  createdAt: DateTime,
  lastModifiedAt: DateTime,
  email: EmailAddress,
  firstName: String,
  lastName: Option[String],
  profileImage: Option[String]
)
