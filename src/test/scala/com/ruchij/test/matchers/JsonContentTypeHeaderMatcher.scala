package com.ruchij.test.matchers

import org.http4s.Response
import org.http4s.headers.`Content-Type`
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.language.higherKinds

class JsonContentTypeHeaderMatcher[F[_]] extends Matcher[Response[F]] {
  override def apply(left: Response[F]): MatchResult =
    MatchResult(
      left.headers.get(`Content-Type`).map(_.value).contains("application/json"),
      s"Headers did NOT contain ContentType: application/json",
      s"Headers did contain ContentType: application/json"
    )
}
