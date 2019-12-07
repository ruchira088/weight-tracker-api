package com.ruchij.daos.authenticationfailure.models

import java.util.UUID

import org.joda.time.DateTime

case class DatabaseAuthenticationFailure(userId: UUID, failedAt: DateTime)
