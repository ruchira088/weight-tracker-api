package com.ruchij.web.routes

import java.util.UUID

import cats.effect.IO
import com.ruchij.test.TestHttpApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.JsonUtils.{json, jsonPostRequest}
import com.ruchij.test.utils.Providers.{clock, contextShift}
import com.ruchij.test.utils.RandomGenerator
import com.ruchij.types.Random
import io.circe.Json
import io.circe.literal._
import org.http4s.{HttpApp, Method, Status}
import org.scalatest.{FlatSpec, MustMatchers}

class UserRoutesSpec extends FlatSpec with MustMatchers {

  "POST /user" should "successfully create a user" in {
    val uuid = UUID.randomUUID()

    implicit val randomUuid: Random[IO, UUID] = new Random[IO, UUID] {
      override def value[B >: UUID]: IO[B] = IO(uuid)
    }

    val httpApp: HttpApp[IO] = TestHttpApp[IO]()

    val username = RandomGenerator.username().value
    val password = RandomGenerator.password().value
    val email = RandomGenerator.email().value
    val firstName = RandomGenerator.option(RandomGenerator.firstName()).value
    val lastName = RandomGenerator.option(RandomGenerator.lastName()).value

    val requestBody: Json =
      json"""{
        "username": $username, "password": $password, "email": $email, "firstName": $firstName, "lastName": $lastName
      }"""

    val expectedResponse =
      json"""{
        "id": $uuid, "username": $username, "email": $email, "firstName": $firstName, "lastName": $lastName
      }"""

    val response = httpApp.run(jsonPostRequest[IO](Method.POST, "/user", requestBody)).unsafeRunSync()

    response.status mustBe Status.Created
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponse)
  }
}
