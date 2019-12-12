package com.ruchij.exceptions

import java.util.UUID

case class LockedUserAccountException(userId: UUID) extends Exception {
  override def getMessage: String = "User account is locked"
}
