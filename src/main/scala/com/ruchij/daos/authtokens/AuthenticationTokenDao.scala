package com.ruchij.daos.authtokens

import java.util.UUID

import cats.data.OptionT
import com.ruchij.services.authentication.models.AuthenticationToken

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

trait AuthenticationTokenDao[F[_]] {
  def createToken(authenticationToken: AuthenticationToken): F[AuthenticationToken]

  def findByUserIdAndSecret(userId: UUID, authenticationSecret: UUID): OptionT[F, AuthenticationToken]

  def findByUserId(userId: UUID): F[List[AuthenticationToken]]

  def extendExpiry(userId: UUID, authenticationSecret: UUID, duration: FiniteDuration): F[AuthenticationToken]

  def remove(userId: UUID, authenticationSecret: UUID): F[AuthenticationToken]
}
