package com.ruchij.web.routes

import java.util.UUID

import cats.effect.IO
import com.ruchij.test.TestHttpApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.JsonUtils.json
import com.ruchij.test.utils.RequestUtils.{authenticatedRequest, jsonRequest}
import com.ruchij.test.utils.Providers.{clock, contextShift}
import com.ruchij.test.utils.RandomGenerator
import com.ruchij.types.Random
import io.circe.Json
import io.circe.literal._
import org.http4s.{HttpApp, Method, Request, Status, Uri}
import org.scalatest.{FlatSpec, MustMatchers}

class UserRoutesSpec extends FlatSpec with MustMatchers {

  "POST /user" should "successfully create a user" in {
    val uuid = UUID.randomUUID()

    implicit val randomUuid: Random[IO, UUID] = new Random[IO, UUID] {
      override def value[B >: UUID]: IO[B] = IO(uuid)
    }

    val httpApp: HttpApp[IO] = TestHttpApp[IO]().httpApp

    val username = RandomGenerator.username()
    val password = RandomGenerator.password()
    val email = RandomGenerator.email()
    val firstName = RandomGenerator.option(RandomGenerator.firstName())
    val lastName = RandomGenerator.option(RandomGenerator.lastName())

    val requestBody: Json =
      json"""{
        "username": $username,
        "password": $password,
        "email": $email,
        "firstName": $firstName,
        "lastName": $lastName
      }"""

    val response = httpApp.run(jsonRequest[IO](Method.POST, "/user", requestBody)).unsafeRunSync()

    val expectedResponse =
      json"""{
        "id": $uuid,
        "username": $username,
        "email": $email,
        "firstName": $firstName,
        "lastName": $lastName
      }"""

    response.status mustBe Status.Created
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponse)
  }

  it should "return a conflict error response if the username already exists" in {
    val databaseUser = RandomGenerator.databaseUser()

    val httpApp = TestHttpApp[IO]().withUser(databaseUser).httpApp

    val requestBody =
      json"""{
        "username": ${databaseUser.username},
        "password": ${databaseUser.password},
        "email": ${databaseUser.email}
      }"""

    val response = httpApp.run(jsonRequest[IO](Method.POST, "/user", requestBody)).unsafeRunSync()

    val expectedResponse =
      json"""{
        "errorMessages": [ ${"username already exists: " + databaseUser.username} ]
      }"""

    response.status mustBe Status.Conflict
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponse)
  }

  it should "return a conflict error response if the email address already exists" in {
    val databaseUser = RandomGenerator.databaseUser()

    val httpApp = TestHttpApp[IO]().withUser(databaseUser).httpApp

    val username = RandomGenerator.username()
    val password = RandomGenerator.password()

    val requestBody =
      json"""{
        "username": $username,
        "password": $password,
        "email": ${databaseUser.email}
      }"""

    val response = httpApp.run(jsonRequest(Method.POST, "/user", requestBody)).unsafeRunSync()

    val expectedResponse =
      json"""{
        "errorMessages": [ ${"email already exists: " + databaseUser.email} ]
      }"""

    response.status mustBe Status.Conflict
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponse)
  }

  "GET /user" should "return the authenticated user" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.authenticationToken(databaseUser.id)

    val httpApp = TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken).httpApp

    val request = authenticatedRequest(authenticationToken, Request[IO](uri = Uri(path = "/user")))

    val response = httpApp.run(request).unsafeRunSync()

    val expectedResponse: Json =
      json"""{
        "id": ${databaseUser.id},
        "username": ${databaseUser.username},
        "email": ${databaseUser.email},
        "firstName": ${databaseUser.firstName},
        "lastName": ${databaseUser.lastName}
      }"""

    response.status mustBe Status.Ok
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponse)
  }
}
