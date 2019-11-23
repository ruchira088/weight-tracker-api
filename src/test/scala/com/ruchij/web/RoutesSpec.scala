package com.ruchij.web

import java.util.UUID

import cats.effect.IO
import com.ruchij.test.HttpTestApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.Providers._
import com.ruchij.test.utils.RandomGenerator
import com.ruchij.test.utils.RequestUtils.{getRequest, jsonRequest}
import com.ruchij.types.FunctionKTypes._
import com.ruchij.types.Random
import com.ruchij.web.routes.Paths.{`/health`, `/session`, `/v1`}
import com.ruchij.web.headers.`X-Correlation-ID`
import io.circe.Json
import io.circe.literal._
import fs2.Stream
import org.http4s.{Headers, MediaType, Method, Request, Response, Status, Uri}
import org.http4s.headers.`Content-Type`
import org.scalatest.{FlatSpec, MustMatchers}

class RoutesSpec extends FlatSpec with MustMatchers {

  "Making a request with missing fields in the request body" should "return a bad request error response" in {
    val application: HttpTestApp[IO] = HttpTestApp[IO]()

    val jsonRequestBody: Json =
      json"""{
        "email": ${RandomGenerator.email().toString}
      }"""

    val request = jsonRequest[IO](Method.POST, `/v1` + `/session`, jsonRequestBody)

    val response: Response[IO] = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ ${"Invalid message body: Could not decode JSON: " + jsonRequestBody} ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.BadRequest)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  "Making a malformed JSON request" should "return a bad request error response" in {
    val application: HttpTestApp[IO] = HttpTestApp[IO]()

    val requestBody =
      """{
        | "name": hello World
        |}""".stripMargin

    val request =
      Request(
        method = Method.POST,
        uri = Uri(path = `/v1` + `/session`),
        headers = Headers.of(`Content-Type`(MediaType.application.json), `X-Correlation-ID`.from(RandomGenerator.uuid().toString)),
        body = Stream.fromIterator[IO](requestBody.getBytes.toIterator)
      )

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Malformed message body: Invalid JSON" ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.BadRequest)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  "Making a request to a valid URL but invalid HTTP method" should "return a 404 error response" in {
    val application = HttpTestApp[IO]()

    val request = getRequest[IO]("/random-path")

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Endpoint not found: GET /random-path" ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.NotFound)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  "Making a request without X-Correlation-ID" should "insert a X-Correlation-ID header when it results in a success response" in {
    val uuid = RandomGenerator.uuid()
    implicit val randomUuid: Random[IO, UUID] = random[IO, UUID](uuid)

    val application = HttpTestApp[IO]()

    val request = Request[IO](uri = Uri(path = `/health`))

    val response = application.httpApp.run(request).unsafeRunSync()

    response must haveStatus(Status.Ok)
    response.headers.get(`X-Correlation-ID`) mustBe Some(`X-Correlation-ID`.from(uuid.toString))

    application.shutdown()
  }

  it should "insert a X-Correlation-ID header when it results in a 404 (not-found) response" in {
    val uuid = RandomGenerator.uuid()
    implicit val randomUuid: Random[IO, UUID] = random[IO, UUID](uuid)

    val application = HttpTestApp[IO]()

    val request = Request[IO](uri = Uri(path = "/not-existing-path"))

    val response = application.httpApp.run(request).unsafeRunSync()

    response must haveStatus(Status.NotFound)
    response.headers.get(`X-Correlation-ID`) mustBe Some(`X-Correlation-ID`.from(uuid.toString))

    application.shutdown()
  }
}
