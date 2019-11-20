package com.ruchij.test.matchers

import com.ruchij.web.headers.`X-Correlation-ID`
import org.http4s.{Request, Response}
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.language.higherKinds

class CorrelationIdMatcher[F[_]](request: Request[F]) extends Matcher[Response[F]] {
  override def apply(response: Response[F]): MatchResult =
    MatchResult(
      request.headers.get(`X-Correlation-ID`)
        .exists {
          requestId => response.headers.get(`X-Correlation-ID`).contains(requestId)
        },
      s"""
         |Request: ${request.headers.get(`X-Correlation-ID`).map(_.value).getOrElse("Missing X-Correlation-ID header")}
         |
         |Response: ${response.headers.get(`X-Correlation-ID`).map(_.value).getOrElse("Response should always have a X-Correlation-ID")}
         |""".stripMargin,
      "X-Correlation-IDs are equal"
    )
}
