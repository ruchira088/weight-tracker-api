package com.ruchij.test.matchers

import cats.effect.Sync
import cats.~>
import com.ruchij.test.utils.JsonUtils
import com.ruchij.types.UnsafeCopoint
import io.circe.Json
import org.http4s.Response
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}

import scala.language.higherKinds

class ResponseJsonPropertyMatcher[F[_]: Sync: UnsafeCopoint: Lambda[X[_] => Either[Throwable, *] ~> X]](expectedJson: Json)
    extends HavePropertyMatcher[Response[F], Json] {

  override def apply(actual: Response[F]): HavePropertyMatchResult[Json] = {
    val actualJson = UnsafeCopoint.unsafeExtract(JsonUtils.json(actual))

    HavePropertyMatchResult(
      actualJson == expectedJson,
      "JSON response",
      expectedJson,
      actualJson
    )
  }
}
