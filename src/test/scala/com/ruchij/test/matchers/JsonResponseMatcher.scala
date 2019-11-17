package com.ruchij.test.matchers

import cats.effect.Sync
import cats.~>
import com.ruchij.test.utils.JsonUtils
import com.ruchij.types.UnsafeCopoint
import io.circe.Json
import org.http4s.Response
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.language.higherKinds

class JsonResponseMatcher[F[_]: Sync: UnsafeCopoint: Either[Throwable, *] ~> *[_]](json: Json)
    extends Matcher[Response[F]] {

  override def apply(response: Response[F]): MatchResult = {
    val actual = UnsafeCopoint.unsafeExtract(JsonUtils.json(response))

    MatchResult(
      json == actual,
      s"""
        |Expected: $json
        |
        |Actual: $actual
        |""".stripMargin,
      "JSON value are equal"
    )
  }
}
