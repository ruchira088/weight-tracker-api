package com.ruchij.test.utils

import cats.effect.Sync
import cats.implicits._
import cats.~>
import io.circe.Json
import io.circe.parser.parse
import org.http4s.Response

import scala.language.higherKinds

object JsonUtils {

  def json[F[_]: Sync: Lambda[X[_] => Either[Throwable, *] ~> X]](response: Response[F]): F[Json] =
    response.bodyAsText.compile.string
      .flatMap(_.parseAsJson)

  implicit class JsonParser(val string: String) extends AnyVal {
    def parseAsJson[F[_]](implicit functionK: Either[Throwable, *] ~> F): F[Json] =
      functionK(parse(string))
  }
}
