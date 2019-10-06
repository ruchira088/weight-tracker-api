package com.ruchij.test.utils

import cats.effect.Sync
import com.ruchij.services.authentication.models.AuthenticationToken
import fs2.Stream
import io.circe.Json
import org.http4s.Credentials.Token
import org.http4s.headers.{Authorization, `Content-Type`}
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Headers, MediaType, Method, Request, Uri}

import scala.language.higherKinds

object RequestUtils {

  def jsonRequest[F[_]: Sync](method: Method, url: String, body: Json): Request[F] =
    Request(
      method,
      uri = Uri(path = url),
      headers = Headers.of(`Content-Type`(MediaType.application.json)),
      body = Stream.fromIterator[F](body.toString.map(_.toByte).toIterator)
    )

  def authenticatedRequest[F[_]](authenticationToken: AuthenticationToken, request: Request[F]): Request[F] =
    request.withHeaders {
      Authorization(Token(CaseInsensitiveString("Bearer"), authenticationToken.secret))
    }
}
