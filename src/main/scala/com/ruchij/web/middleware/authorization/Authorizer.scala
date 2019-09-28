package com.ruchij.web.middleware.authorization

import java.util.UUID

import cats.Monad
import cats.effect.Sync
import com.ruchij.services.user.models.User
import com.ruchij.services.authorization.{AuthorizationService, Permission}
import org.http4s.Response

import scala.language.higherKinds

object Authorizer {

  def authorize[F[_]: Sync](
    authenticatedUser: User, userId: UUID, permission: Permission
  )(block: => F[Response[F]])(implicit authorizationService: AuthorizationService[F]): F[Response[F]] =
    Monad[F].flatMap(authorizationService.isAuthorized(authenticatedUser, userId, permission)) {
      if (_) block else Sync[F].raiseError(???)
    }
}
