package com.ruchij.web.middleware.authentication

import cats.Monad
import cats.data.{Kleisli, OptionT}
import com.ruchij.services.user.models.User
import com.ruchij.services.authentication.AuthenticationService
import org.http4s.Request
import org.http4s.server._

import scala.language.higherKinds

object RequestAuthenticator {

  private def authUser[F[_]: Monad](
    authenticationService: AuthenticationService[F],
    tokenExtractor: AuthenticationTokenExtractor[F]
  ): Kleisli[OptionT[F, *], Request[F], User] =
    Kleisli[OptionT[F, *], Request[F], User] { request =>
      for {
        secret <- tokenExtractor.extract(request)
        user <- OptionT.liftF(authenticationService.authenticate(secret))
      } yield user
    }

  def authenticationMiddleware[F[_]: Monad](
    authenticationService: AuthenticationService[F],
    tokenExtractor: AuthenticationTokenExtractor[F]
  ): AuthMiddleware[F, User] =
    AuthMiddleware(authUser(authenticationService, tokenExtractor))
}
