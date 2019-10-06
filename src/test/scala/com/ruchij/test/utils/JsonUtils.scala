package com.ruchij.test.utils

import cats.effect.Sync
import cats.implicits._
import com.ruchij.types.Transformation
import com.ruchij.types.Transformation.~>
import fs2.Stream
import io.circe.Json
import io.circe.parser.parse
import org.http4s.headers.`Content-Type`
import org.http4s.{Headers, MediaType, Method, Request, Response, Uri}

import scala.language.higherKinds

object JsonUtils {

  def jsonPostRequest[F[_]: Sync](method: Method, url: String, body: Json): Request[F] =
    Request(
      method,
      uri = Uri(path = url),
      headers = Headers.of(`Content-Type`(MediaType.application.json)),
      body = Stream.fromIterator[F](body.toString.map(_.toByte).toIterator)
    )

  def json[F[_]: Sync: Lambda[X[_] => Either[Throwable, *] ~> X]](response: Response[F]): F[Json] =
    response.body.compile.toVector
      .map(_.map(_.toChar).mkString)
      .flatMap(_.parseAsJson)

  implicit class JsonParser(val string: String) extends AnyVal {
    def parseAsJson[F[_]: Lambda[X[_] => Either[Throwable, *] ~> X]]: F[Json] =
      Transformation[Either[Throwable, *], F].apply(parse(string))
  }
}
