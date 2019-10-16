package com.ruchij.web.middleware.authorization

import java.util.UUID

import cats.Monad
import cats.effect.Sync
import com.ruchij.exceptions.AuthorizationException
import com.ruchij.services.user.models.User
import com.ruchij.services.authorization.{AuthorizationService, Permission}
import org.http4s.Response

import scala.language.higherKinds

object Authorizer {

  def authorize[F[_]: Sync](authorizationService: AuthorizationService[F])(
    authenticatedUser: User, userId: UUID, permission: Permission
  )(block: => F[Response[F]]): F[Response[F]] =
    Monad[F].flatMap(authorizationService.isAuthorized(authenticatedUser, userId, permission)) {
      if (_) block else Sync[F].raiseError(AuthorizationException(s"$permission permissions not found for $userId"))
    }
}
