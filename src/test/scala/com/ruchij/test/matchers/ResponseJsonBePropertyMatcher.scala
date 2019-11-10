package com.ruchij.test.matchers

import org.http4s.Response
import org.http4s.headers.`Content-Type`
import org.scalatest.matchers.{BePropertyMatchResult, BePropertyMatcher}

import scala.language.higherKinds

class ResponseJsonBePropertyMatcher[F[_]] extends BePropertyMatcher[Response[F]] {
  override def apply(response: Response[F]): BePropertyMatchResult =
    BePropertyMatchResult(
      response.headers.get(`Content-Type`).map(_.value).contains("application/json"),
      "JSON response"
    )
}
