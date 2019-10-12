package com.ruchij.web.routes

import java.util.UUID

import cats.effect.IO
import com.ruchij.test.TestHttpApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.JsonUtils.json
import com.ruchij.test.utils.Providers.{clock, contextShift}
import com.ruchij.test.utils.RandomGenerator
import com.ruchij.test.utils.RequestUtils.{authenticatedRequest, getRequest, jsonRequest}
import com.ruchij.types.Random
import com.ruchij.web.routes.Paths.`/user`
import io.circe.Json
import io.circe.literal._
import org.http4s.{Method, Response, Status}
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

    val expectedJsonResponse =
      json"""{
        "id": $uuid,
        "email": $email,
        "firstName": $firstName,
        "lastName": $lastName
      }"""

    println("_____________________________")
    println(json(response).unsafeRunSync())
    println("_____________________________")

    response.status mustBe Status.Created
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)

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

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ ${s"email already exists: ${databaseUser.email}"} ]
      }"""

    response.status mustBe Status.Conflict
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)

    application.shutdown()
  }

  "GET /user" should "return the authenticated user" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.authenticationToken(databaseUser.id)

    val application: TestHttpApp[IO] =
      TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val request = authenticatedRequest(authenticationToken, getRequest[IO](`/user`))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse: Json =
      json"""{
        "id": ${databaseUser.id},
        "email": ${databaseUser.email},
        "firstName": ${databaseUser.firstName},
        "lastName": ${databaseUser.lastName}
      }"""

    response.status mustBe Status.Ok
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)

    application.shutdown()
  }

  it should "return error response when the Authorization header is missing" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.authenticationToken(databaseUser.id)

    val application = TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val request = getRequest[IO](`/user`)

    val response: Response[IO] = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Missing Authorization header" ]
      }"""

    response.status mustBe Status.Unauthorized
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)

    application.shutdown()
  }

  "GET /user/:userId" should "return the user with userId" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.authenticationToken(databaseUser.id)

    val application = TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val request = authenticatedRequest(authenticationToken, getRequest[IO](s"${`/user`}/${databaseUser.id}"))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonBody =
      json"""{
        "id": ${databaseUser.id},
        "email": ${databaseUser.email},
        "firstName": ${databaseUser.firstName},
        "lastName": ${databaseUser.lastName}
      }"""

    response.status mustBe Status.Ok
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonBody)

    application.shutdown()
  }
}
