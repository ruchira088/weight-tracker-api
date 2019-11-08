package com.ruchij.web

import cats.effect.IO
import com.ruchij.test.TestHttpApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.Providers._
import com.ruchij.test.utils.RandomGenerator
import com.ruchij.test.utils.RequestUtils.jsonRequest
import com.ruchij.test.utils.JsonUtils.json
import com.ruchij.web.routes.Paths.`/session`
import io.circe.Json
import io.circe.literal._
import fs2.Stream
import org.http4s.{Headers, MediaType, Method, Request, Response, Status, Uri}
import org.http4s.headers.`Content-Type`
import org.scalatest.{FlatSpec, MustMatchers}

class RoutesSpec extends FlatSpec with MustMatchers {

  "Making a request with missing fields in the request body" should "return a bad request error response" in {
    val application: TestHttpApp[IO] = TestHttpApp[IO]()

    val jsonRequestBody: Json =
      json"""{
        "email": ${RandomGenerator.email().toString}
      }"""

    val request = jsonRequest[IO](Method.POST, `/session`, jsonRequestBody)

    val response: Response[IO] = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ ${"Invalid message body: Could not decode JSON: " + jsonRequestBody} ]
      }"""

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)
    response.status mustBe Status.BadRequest

    application.shutdown()
  }

  "Making a malformed JSON request" should "return a bad request error response" in {
    val application: TestHttpApp[IO] = TestHttpApp[IO]()

    val requestBody =
      """{
        | "name": hello World
        |}""".stripMargin

    val request =
      Request(
        method = Method.POST,
        uri = Uri(path = `/session`),
        headers = Headers.of(`Content-Type`(MediaType.application.json)),
        body = Stream.fromIterator[IO](requestBody.getBytes.toIterator)
      )

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Malformed message body: Invalid JSON" ]
      }"""

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)
    response.status mustBe Status.BadRequest

    application.shutdown()
  }
}