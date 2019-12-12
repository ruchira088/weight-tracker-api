package com.ruchij.daos.authenticationfailure.models

import java.util.UUID

import org.joda.time.DateTime

case class DatabaseAuthenticationFailure(id: UUID, userId: UUID, failedAt: DateTime, deleted: Boolean)
