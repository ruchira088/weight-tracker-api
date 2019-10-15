package com.ruchij.daos.authtokens

import cats.data.OptionT
import com.ruchij.daos.authtokens.models.DatabaseAuthenticationToken

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

trait AuthenticationTokenDao[F[_]] {
  def createToken(databaseAuthenticationToken: DatabaseAuthenticationToken): F[DatabaseAuthenticationToken]

  def find(secret: String): OptionT[F, DatabaseAuthenticationToken]

  def extendExpiry(secret: String, duration: FiniteDuration): F[DatabaseAuthenticationToken]

  def remove(secret: String): F[DatabaseAuthenticationToken]
}
