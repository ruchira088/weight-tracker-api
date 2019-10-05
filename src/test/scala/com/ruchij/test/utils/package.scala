package com.ruchij.test

import cats.Functor
import cats.effect.Sync
import cats.implicits._
import com.ruchij.types.Transformation
import com.ruchij.types.Transformation.~>
import io.circe.Json
import io.circe.parser.parse
import org.http4s.{HttpRoutes, Request, Response}

import scala.language.higherKinds

package object utils {

  def responseEval[F[_]: Functor](httpRoutes: HttpRoutes[F], request: Request[F]): F[Response[F]] =
    httpRoutes.run(request).getOrElse(Response.notFound)

  def json[F[_]: Sync: Lambda[X[_] => Either[Throwable, *] ~> X]](response: Response[F]): F[Json] =
    response.body.compile.toVector
      .map(_.map(_.toChar).mkString)
      .flatMap(_.parseAsJson)

  implicit class JsonParser(val string: String) extends AnyVal {
    def parseAsJson[F[_]: Lambda[X[_] => Either[Throwable, *] ~> X]]: F[Json] =
      Transformation[Either[Throwable, *], F].apply(parse(string))
  }
}
