package com.ruchij.web.routes

import cats.effect.IO
import com.ruchij.test.TestHttpApp
import com.ruchij.test.utils.Providers.{clock, contextShift}
import com.ruchij.test.utils.RandomGenerator
import com.ruchij.test.utils.RequestUtils.jsonRequest
import com.ruchij.test.matchers.{beJsonResponse, matchWith}
import com.ruchij.test.utils.JsonUtils.json
import com.ruchij.web.routes.Paths.`/session`
import io.circe.Json
import io.circe.literal._
import org.http4s.{Method, Response, Status}
import org.scalatest.{FlatSpec, MustMatchers}

class SessionRoutesSpec extends FlatSpec with MustMatchers {

  "/session" should "successfully create an authentication token for valid credentials" in {
    val databaseUser = RandomGenerator.databaseUser()

    val application = TestHttpApp[IO]().withUser(databaseUser)

    val requestBody: Json =
      json"""{
        "email": ${databaseUser.email},
        "password": ${RandomGenerator.PASSWORD}
      }"""

    val response = application.httpApp.run(jsonRequest(Method.POST, `/session`, requestBody)).unsafeRunSync()

    response.status mustBe Status.Created
    response must beJsonResponse[IO]

    application.shutdown()
  }

  it should "return an authorized error response for an incorrect password" in {
    val databaseUser = RandomGenerator.databaseUser()

    val application: TestHttpApp[IO] = TestHttpApp[IO]().withUser(databaseUser)

    val requestBody: Json =
      json"""{
        "email": ${databaseUser.email},
        "password": ${RandomGenerator.password()}
      }"""

    val response: Response[IO] =
      application.httpApp.run(jsonRequest(Method.POST, `/session`, requestBody)).unsafeRunSync()

    val expectedJsonResponse: Json =
      json"""{
        "errorMessages": [ "Invalid credentials" ]
      }"""

    response.status mustBe Status.Unauthorized
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)

    application.shutdown()
  }

  it should "return not found response (404) when the email doesn't exist" in {
    val application: TestHttpApp[IO] = TestHttpApp[IO]()

    val email = RandomGenerator.email()
    val requestBody: Json =
      json"""{
        "email": $email,
        "password": ${RandomGenerator.password()}
      }"""

    val response = application.httpApp.run(jsonRequest(Method.POST, `/session`, requestBody)).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ ${s"Email not found: $email"} ]
      }"""

    response.status mustBe Status.NotFound
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)

    application.shutdown()
  }
}
