package com.ruchij.web.middleware.authentication

import cats.implicits._
import cats.Applicative
import cats.data.OptionT
import cats.effect.Sync
import com.ruchij.exceptions.AuthenticationException
import org.http4s.Request
import org.http4s.headers.Authorization

import scala.language.higherKinds
import scala.util.matching.Regex

trait AuthenticationTokenExtractor[F[_]] {
  def extract(request: Request[F]): OptionT[F, String]
}

object AuthenticationTokenExtractor {
  private val bearerTokenRegex: Regex = "[Bb]earer (\\S+)".r

  def bearerTokenExtractor[F[_]: Sync]: AuthenticationTokenExtractor[F] =
    (request: Request[F]) =>
      OptionT.liftF {
        request.headers.get(Authorization).fold[F[Authorization.HeaderT]] {
          Sync[F].raiseError(AuthenticationException("Missing Authorization header"))
        }(Applicative[F].pure)
          .map {
            _.credentials.renderString
          }
          .flatMap {
            case bearerTokenRegex(token) => Applicative[F].pure(token)
            case _ => Sync[F].raiseError(AuthenticationException("Unable to extract Bearer token"))
          }
    }
}
