package com.ruchij.test

import cats.effect.IO
import com.ruchij.types.FunctionKTypes.eitherThrowableToIO
import io.circe.Json
import org.http4s.{MediaType, Status}

package object matchers {

  val beJsonContentType: ResponseContentTypeMatcher[IO] =
    new ResponseContentTypeMatcher[IO](MediaType.application.json)

  def haveJson(json: Json): JsonResponseMatcher[IO] = new JsonResponseMatcher[IO](json)

  def haveStatus(status: Status): ResponseStatusMatcher[IO] = new ResponseStatusMatcher[IO](status)
}
