package com.ruchij.test.utils

import cats.effect.Sync
import com.ruchij.web.headers.`X-Correlation-ID`
import com.ruchij.test.utils.RandomGenerator.uuid
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
      headers = Headers.of(`Content-Type`(MediaType.application.json), `X-Correlation-ID`.from(uuid().toString)),
      body = Stream.fromIterator[F](body.toString.getBytes.toIterator)
    )

  def getRequest[F[_]](path: String, headers: Headers = Headers.of(`X-Correlation-ID`.from(uuid().toString))): Request[F] =
    Request[F](uri = Uri(path = path), headers = headers)

  def authenticatedRequest[F[_]](secret: String, request: Request[F]): Request[F] =
    request.putHeaders {
      Authorization(Token(CaseInsensitiveString("Bearer"), secret))
    }
}
