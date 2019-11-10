package com.ruchij.test.utils

import cats.effect.Sync
import cats.implicits._
import cats.~>
import io.circe.Json
import io.circe.parser.parse
import org.http4s.Response

import scala.language.higherKinds

object JsonUtils {

  def json[F[_]: Sync](response: Response[F])(implicit functionK: Either[Throwable, *] ~> F): F[Json] =
    response.bodyAsText.compile[F, F, String].string
      .flatMap { jsonString => functionK(parse(jsonString)) }
}
