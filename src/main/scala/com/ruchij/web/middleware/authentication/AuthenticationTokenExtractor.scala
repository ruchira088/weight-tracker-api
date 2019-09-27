package com.ruchij.web.middleware.authentication

import cats.Applicative
import cats.data.OptionT
import org.http4s.Request
import org.http4s.headers.Authorization

import scala.language.higherKinds
import scala.util.matching.Regex

trait AuthenticationTokenExtractor[F[_]] {
  def extract(request: Request[F]): OptionT[F, String]
}

object AuthenticationTokenExtractor {
  private val bearerTokenRegex: Regex = "[Bb]earer (\\S+)".r

  def bearerTokenExtractor[F[_]: Applicative]: AuthenticationTokenExtractor[F] =
    (request: Request[F]) =>
      OptionT.fromOption {
        for {
          header <- request.headers.get(Authorization)
          bearerTokenRegex(token) <- Option(header.credentials.renderString)
        }
        yield token
      }
}
