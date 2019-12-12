package com.ruchij.daos.authenticationfailure

import java.util.UUID

import com.ruchij.daos.authenticationfailure.models.DatabaseAuthenticationFailure
import org.joda.time.DateTime

import scala.language.higherKinds

trait AuthenticationFailureDao[F[_]] {
  def insert(databaseAuthenticationFailure: DatabaseAuthenticationFailure): F[Boolean]

  def findByUser(userId: UUID, after: DateTime): F[List[DatabaseAuthenticationFailure]]

  def delete(id: UUID): F[Boolean]
}
