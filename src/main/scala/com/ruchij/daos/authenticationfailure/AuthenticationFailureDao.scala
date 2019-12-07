package com.ruchij.daos.authenticationfailure

import com.ruchij.daos.authenticationfailure.models.DatabaseAuthenticationFailure

import scala.language.higherKinds

trait AuthenticationFailureDao[F[_]] {
  def insert(databaseAuthenticationFailure: DatabaseAuthenticationFailure): F[Boolean]
}
