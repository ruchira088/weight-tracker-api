package com.ruchij.daos.authentication

import java.util.UUID

import cats.data.OptionT
import com.ruchij.daos.authentication.models.UserAuthenticationConfiguration

import scala.language.higherKinds

trait UserAuthenticationConfigurationDao[F[_]] {
  def insert(authenticationConfiguration: UserAuthenticationConfiguration): F[Boolean]

  def findByUserId(userId: UUID): OptionT[F, UserAuthenticationConfiguration]

  def updatePassword(userId: UUID, password: String): F[Boolean]

  def updateTotpSecret(userId: UUID, totpSecret: Option[String]): F[Boolean]
}
