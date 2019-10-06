package com.ruchij.test.utils

import cats.effect.Sync
import cats.implicits._
import com.ruchij.types.Transformation
import com.ruchij.types.Transformation.~>
import io.circe.Json
import io.circe.parser.parse
import org.http4s.Response

import scala.language.higherKinds

object JsonUtils {

  def json[F[_]: Sync: Lambda[X[_] => Either[Throwable, *] ~> X]](response: Response[F]): F[Json] =
    response.body.compile.toVector
      .map(_.map(_.toChar).mkString)
      .flatMap(_.parseAsJson)

  implicit class JsonParser(val string: String) extends AnyVal {
    def parseAsJson[F[_]: Lambda[X[_] => Either[Throwable, *] ~> X]]: F[Json] =
      Transformation[Either[Throwable, *], F].apply(parse(string))
  }
}
