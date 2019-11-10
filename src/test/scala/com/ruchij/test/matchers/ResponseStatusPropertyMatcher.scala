package com.ruchij.test.matchers

import org.http4s.{Response, Status}
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}

import scala.language.higherKinds

class ResponseStatusPropertyMatcher[F[_]](expectedStatus: Status) extends HavePropertyMatcher[Response[F], Status] {
  override def apply(response: Response[F]): HavePropertyMatchResult[Status] =
    HavePropertyMatchResult(
      expectedStatus == response.status,
      "Response Status Code",
      expectedStatus,
      response.status
    )
}
