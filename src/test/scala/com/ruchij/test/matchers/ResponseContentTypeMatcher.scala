package com.ruchij.test.matchers

import org.http4s.{MediaType, Response}
import org.http4s.headers.`Content-Type`
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.language.higherKinds

class ResponseContentTypeMatcher[F[_]](mediaType: MediaType) extends Matcher[Response[F]] {

  override def apply(response: Response[F]): MatchResult = {
    val actual = response.headers.get(`Content-Type`).map(_.mediaType)

    MatchResult(
      actual.contains(mediaType),
      s"""
        |Expected: $mediaType
        |
        |Actual: ${actual.map(_.toString).getOrElse("""Missing "Content-Type" Header""")}
        |""".stripMargin,
      s"Content-Type is $mediaType"
    )
  }

}
