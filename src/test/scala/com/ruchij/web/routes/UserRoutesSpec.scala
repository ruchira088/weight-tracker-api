package com.ruchij.web.routes

import java.util.UUID

import cats.effect.IO
import com.ruchij.circe.Encoders.jodaTimeEncoder
import com.ruchij.test.TestHttpApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.JsonUtils.json
import com.ruchij.test.utils.Providers.{clock, contextShift}
import com.ruchij.test.utils.RandomGenerator
import com.ruchij.test.utils.RequestUtils.{authenticatedRequest, getRequest, jsonRequest}
import com.ruchij.types.Random
import com.ruchij.web.routes.Paths.{`/user`, `weight-entry`}
import io.circe.Json
import io.circe.literal._
import org.http4s.{Method, Request, Response, Status, Uri}
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, MustMatchers}

class UserRoutesSpec extends FlatSpec with MustMatchers {

  "POST /user" should "successfully create a user" in {
    val uuid = UUID.randomUUID()
    implicit val randomUuid: Random[IO, UUID] = RandomGenerator.random(uuid)

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

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)
    response.status mustBe Status.Created

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

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)
    response.status mustBe Status.Conflict

    application.shutdown()
  }

  "GET /user" should "return the authenticated user" in {
    val databaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val application: TestHttpApp[IO] =
      TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(databaseAuthenticationToken)

    val request = authenticatedRequest(databaseAuthenticationToken.secret, getRequest[IO](`/user`))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse: Json =
      json"""{
        "id": ${databaseUser.id},
        "email": ${databaseUser.email},
        "firstName": ${databaseUser.firstName},
        "lastName": ${databaseUser.lastName}
      }"""

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)
    response.status mustBe Status.Ok

    application.shutdown()
  }

  it should "return error response when the Authorization header is missing" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val application = TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val request = getRequest[IO](`/user`)

    val response: Response[IO] = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Missing Authorization header" ]
      }"""

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)
    response.status mustBe Status.Unauthorized

    application.shutdown()
  }

  "GET /user/:userId" should "return the user with userId" in {
    val databaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val application = TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(databaseAuthenticationToken)

    val request =
      authenticatedRequest(databaseAuthenticationToken.secret, getRequest[IO](s"${`/user`}/${databaseUser.id}"))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonBody =
      json"""{
        "id": ${databaseUser.id},
        "email": ${databaseUser.email},
        "firstName": ${databaseUser.firstName},
        "lastName": ${databaseUser.lastName}
      }"""

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonBody)
    response.status mustBe Status.Ok

    application.shutdown()
  }

  "POST /user/:userId/weight-entry" should "successfully create a weight entry" in {
    val databaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val uuid = RandomGenerator.uuid()
    implicit val randomUuid: Random[IO, UUID] = RandomGenerator.random(uuid)

    val application = TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(databaseAuthenticationToken)

    val now = DateTime.now()
    val weight = RandomGenerator.weight()
    val description = RandomGenerator.option(RandomGenerator.description())

    val requestBody =
      json"""{
        "timestamp": $now,
        "weight": $weight,
        "description": $description
      }"""

    val request = authenticatedRequest[IO](
      databaseAuthenticationToken.secret,
      jsonRequest(Method.POST, s"${`/user`}/${databaseUser.id}/${`weight-entry`}", requestBody)
    )

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonBody =
      json"""{
        "id": $uuid,
        "userId": ${databaseUser.id},
        "timestamp": $now,
        "weight": $weight,
        "description": $description
      }"""

    json(response) must matchWith(expectedJsonBody)
    response must beJsonResponse[IO]
    response.status mustBe Status.Created

    application.shutdown()
  }

  "GET /user/:userId/weight-entry?page-number=Int&page-size=Int" should "return persisted weight entries" in {
    val databaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val weightEntryOne = RandomGenerator.databaseWeightEntry(databaseUser.id)
    val weightEntryTwo = RandomGenerator.databaseWeightEntry(databaseUser.id)
    val weightEntryThree = RandomGenerator.databaseWeightEntry(databaseUser.id)

    val application =
      TestHttpApp[IO]()
        .withUser(databaseUser)
        .withAuthenticationToken(databaseAuthenticationToken)
        .withWeightEntries(weightEntryOne, weightEntryTwo, weightEntryThree)

    val request = authenticatedRequest[IO](
      databaseAuthenticationToken.secret,
      Request[IO](uri = Uri.unsafeFromString(s"${`/user`}/${databaseUser.id}/${`weight-entry`}?page-number=0&page-size=2"))
    )

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonBody =
      json"""{
        "results": [
          {
            "id": ${weightEntryOne.id},
            "userId": ${weightEntryOne.userId},
            "timestamp": ${weightEntryOne.timestamp},
            "weight": ${weightEntryOne.weight},
            "description": ${weightEntryOne.description}
          },
          {
            "id": ${weightEntryTwo.id},
            "userId": ${weightEntryTwo.userId},
            "timestamp": ${weightEntryTwo.timestamp},
            "weight": ${weightEntryTwo.weight},
            "description": ${weightEntryTwo.description}
          }
        ],
        "pageNumber": 0,
        "pageSize": 2
      }"""

    json(response) must matchWith(expectedJsonBody)
    response must beJsonResponse[IO]
    response.status mustBe Status.Ok

    application.shutdown()
  }
}
