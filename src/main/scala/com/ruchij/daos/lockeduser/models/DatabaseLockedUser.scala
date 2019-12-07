package com.ruchij.daos.lockeduser.models

import java.util.UUID

import org.joda.time.DateTime

case class DatabaseLockedUser(userId: UUID, lockedAt: DateTime, unlockCode: String, unlockedAt: Option[DateTime])
