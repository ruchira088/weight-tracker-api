package com.ruchij.web.routes

import java.util.UUID

import cats.effect.IO
import com.ruchij.test.TestHttpApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.JsonUtils.json
import com.ruchij.test.utils.Providers.{clock, contextShift}
import com.ruchij.test.utils.RandomGenerator
import com.ruchij.test.utils.RequestUtils.{authenticatedRequest, jsonRequest}
import com.ruchij.web.routes.Paths.`/user`
import com.ruchij.types.Random
import io.circe.Json
import io.circe.literal._
import org.http4s.{Method, Request, Status, Uri}
import org.scalatest.{FlatSpec, MustMatchers}

class UserRoutesSpec extends FlatSpec with MustMatchers {

  "POST /user" should "successfully create a user" in {
    val uuid = UUID.randomUUID()

    implicit val randomUuid: Random[IO, UUID] = new Random[IO, UUID] {
      override def value[B >: UUID]: IO[B] = IO(uuid)
    }

    val application: TestHttpApp[IO] = TestHttpApp[IO]()

    val email = RandomGenerator.email()
    val password = RandomGenerator.password()
    val firstName = RandomGenerator.firstName()
    val lastName = RandomGenerator.option(RandomGenerator.lastName())

    val requestBody: Json =
      json"""{
        "email": $email,
        "password": $password,
        "firstName": $firstName,
        "lastName": $lastName
      }"""

    val response = application.httpApp.run(jsonRequest[IO](Method.POST, `/user`, requestBody)).unsafeRunSync()

    val expectedResponse =
      json"""{
        "id": $uuid,
        "email": $email,
        "firstName": $firstName,
        "lastName": $lastName
      }"""

    response.status mustBe Status.Created
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponse)

    application.shutdown()
  }

  it should "return a conflict error response if the email address already exists" in {
    val databaseUser = RandomGenerator.databaseUser()

    val application: TestHttpApp[IO] = TestHttpApp[IO]().withUser(databaseUser)

    val password = RandomGenerator.password()
    val firstName = RandomGenerator.firstName()

    val requestBody =
      json"""{
        "email": ${databaseUser.email},
        "password": $password,
        "firstName": $firstName
      }"""

    val response = application.httpApp.run(jsonRequest(Method.POST, `/user`, requestBody)).unsafeRunSync()

    val expectedResponse =
      json"""{
        "errorMessages": [ ${"email already exists: " + databaseUser.email} ]
      }"""

    response.status mustBe Status.Conflict
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponse)

    application.shutdown()
  }

  "GET /user" should "return the authenticated user" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.authenticationToken(databaseUser.id)

    val application: TestHttpApp[IO] =
      TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val request = authenticatedRequest(authenticationToken, Request[IO](uri = Uri(path = `/user`)))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedResponse: Json =
      json"""{
        "id": ${databaseUser.id},
        "email": ${databaseUser.email},
        "firstName": ${databaseUser.firstName},
        "lastName": ${databaseUser.lastName}
      }"""

    response.status mustBe Status.Ok
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponse)

    application.shutdown()
  }
}
