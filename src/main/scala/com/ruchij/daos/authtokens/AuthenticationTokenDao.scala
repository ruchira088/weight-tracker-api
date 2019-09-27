package com.ruchij.daos.authtokens

import cats.data.OptionT
import com.ruchij.services.authentication.models.AuthenticationToken

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

trait AuthenticationTokenDao[F[_]] {
  def createToken(authenticationToken: AuthenticationToken): F[AuthenticationToken]

  def find(secret: String): OptionT[F, AuthenticationToken]

  def extendExpiry(secret: String, duration: FiniteDuration): F[AuthenticationToken]

  def remove(secret: String): F[AuthenticationToken]
}
