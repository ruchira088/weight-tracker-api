package com.ruchij.daos.authentication.models

import java.util.UUID

import org.joda.time.DateTime

case class UserAuthenticationConfiguration(
  userId: UUID,
  createdAt: DateTime,
  lastModifiedAt: DateTime,
  password: String,
  totpSecret: Option[String]
)
