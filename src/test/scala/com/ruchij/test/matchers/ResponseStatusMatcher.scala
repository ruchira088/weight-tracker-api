package com.ruchij.test.matchers

import org.http4s.{Response, Status}
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.language.higherKinds

class ResponseStatusMatcher[F[_]](status: Status) extends Matcher[Response[F]] {

  override def apply(response: Response[F]): MatchResult =
    MatchResult(
      response.status == status,
      s"""
         |Expected: $status
         |
         |Actual: ${response.status}
         |""".stripMargin,
      s"Expected and actual response status was $status"
    )
}
