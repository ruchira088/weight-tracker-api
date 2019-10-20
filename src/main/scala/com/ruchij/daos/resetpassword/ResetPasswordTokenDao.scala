package com.ruchij.daos.resetpassword

import java.util.UUID

import cats.data.OptionT
import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import org.joda.time.DateTime

import scala.language.higherKinds

trait ResetPasswordTokenDao[F[_]] {
  def insert(databaseResetPassword: DatabaseResetPasswordToken): F[Boolean]

  def find(userId: UUID, secret: String): OptionT[F, DatabaseResetPasswordToken]

  def resetCompleted(userId: UUID, secret: String, timestamp: DateTime): F[Boolean]
}
