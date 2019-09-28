package com.ruchij.services.authorization

import java.util.UUID

import com.ruchij.services.user.models.User

import scala.language.higherKinds

trait AuthorizationService[F[_]] {

  def isAuthorized(authenticatedUser: User, userId: UUID, permission: Permission): F[Boolean]
}
