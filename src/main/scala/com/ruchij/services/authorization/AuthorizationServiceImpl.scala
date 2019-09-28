package com.ruchij.services.authorization

import java.util.UUID

import cats.Applicative
import com.ruchij.services.user.models.User

import scala.language.higherKinds

class AuthorizationServiceImpl[F[_]: Applicative] extends AuthorizationService[F] {

  override def isAuthorized(authenticatedUser: User, userId: UUID, permission: Permission): F[Boolean] =
    Applicative[F].pure(authenticatedUser.id == userId)
}
