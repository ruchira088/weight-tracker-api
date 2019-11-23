package com.ruchij.web.routes

import java.util.UUID

import cats.effect.{Clock, IO}
import com.ruchij.circe.Encoders.{jodaTimeEncoder, taggedStringEncoder}
import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import com.ruchij.services.email.models.Email
import com.ruchij.services.user.models.User
import com.ruchij.test.HttpTestApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.Providers.{clock, contextShift, random}
import com.ruchij.test.utils.{Providers, RandomGenerator}
import com.ruchij.test.utils.RequestUtils.{authenticatedRequest, getRequest, jsonRequest}
import com.ruchij.types.Random
import com.ruchij.types.FunctionKTypes._
import com.ruchij.web.headers.`X-Correlation-ID`
import com.ruchij.web.requests.queryparameters.QueryParameter.{PageNumberQueryParameter, PageSizeQueryParameter}
import com.ruchij.web.routes.Paths.{`/`, `/session`, `/user`, `/v1`, `reset-password`, `weight-entry`}
import io.circe.Json
import io.circe.literal._
import org.http4s.{Method, Request, Response, Status, Uri}
import org.joda.time.{DateTime, Duration}
import org.scalatest.{FlatSpec, MustMatchers, OptionValues}

class UserRoutesSpec extends FlatSpec with MustMatchers with OptionValues {

  s"POST ${`/v1` + `/user`}" should "successfully create a user" in {
    val uuid = UUID.randomUUID()
    implicit val randomUuid: Random[IO, UUID] = random(uuid)

    val application: HttpTestApp[IO] = HttpTestApp[IO]()

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

    val request = jsonRequest[IO](Method.POST, `/v1` + `/user`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "id": $uuid,
        "email": $email,
        "firstName": $firstName,
        "lastName": $lastName
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Created)
    response must haveCorrelationIdOf(request)

    application.externalEmailMailBox.size mustBe 1
    application.externalEmailMailBox.peek mustBe Email.welcomeEmail(User(uuid, email, firstName, lastName))

    application.shutdown()
  }

  it should "ignore the lastName field if it is empty, and successfully create a user" in {
    val uuid = RandomGenerator.uuid()
    implicit val randomUuid: Random[IO, UUID] = random[IO, UUID](uuid)

    val application = HttpTestApp[IO]()

    val email = RandomGenerator.email()
    val password = RandomGenerator.password()
    val firstName = RandomGenerator.firstName()

    val requestBody =
      json"""{
        "email": $email,
        "password": $password,
        "firstName": $firstName,
        "lastName": " "
      }"""

    val request = jsonRequest[IO](Method.POST, `/v1` + `/user`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "id": $uuid,
        "email": $email,
        "firstName": $firstName,
        "lastName": null
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Created)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "return a conflict error response if the email address already exists" in {
    val databaseUser = RandomGenerator.databaseUser()

    val application: HttpTestApp[IO] = HttpTestApp[IO]().withUser(databaseUser)

    val password = RandomGenerator.password()
    val firstName = RandomGenerator.firstName()

    val requestBody =
      json"""{
        "email": ${databaseUser.email},
        "password": $password,
        "firstName": $firstName
      }"""

    val request = jsonRequest[IO](Method.POST, `/v1` + `/user`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ ${s"email already exists: ${databaseUser.email}"} ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Conflict)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "return a validation error response for invalid an invalid email" in {
    val application = HttpTestApp[IO]()

    val requestBody =
      json"""{
        "email": "not.valid",
        "password": ${RandomGenerator.password()},
        "firstName": ${RandomGenerator.firstName()}
      }"""

    val request = jsonRequest[IO](Method.POST, `/v1` + `/user`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "email is not valid" ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.BadRequest)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "return a validation error response for an invalid password" in {
    val application = HttpTestApp[IO]()

    val requestBody =
      json"""{
        "email": ${RandomGenerator.email()},
        "password": "password",
        "firstName": ${RandomGenerator.firstName()}
      }"""

    val request = jsonRequest[IO](Method.POST, `/v1` + `/user`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [
          "password must contain at least one digit",
          "password must contain at least one special character"
        ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.BadRequest)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "return an validation response for multiple invalid parameters" in {
    val application = HttpTestApp[IO]()

    val requestBody =
      json"""{
        "email": "",
        "password": "",
        "firstName": ""
      }"""

    val request = jsonRequest[IO](Method.POST, `/v1` + `/user`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [
          "firstName must not be empty",
          "email is not valid",
          "password must contain at least 8 characters",
          "password must contain at least one letter",
          "password must contain at least one digit",
          "password must contain at least one special character"
        ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.BadRequest)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  s"GET ${`/v1` + `/user` + `/` + ":userId"}" should "return the user with userId" in {
    val databaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val application = HttpTestApp[IO]().withUser(databaseUser).withAuthenticationToken(databaseAuthenticationToken)

    val request =
      authenticatedRequest(databaseAuthenticationToken.secret, getRequest[IO](`/v1` + `/user` + `/` + databaseUser.id))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonBody =
      json"""{
        "id": ${databaseUser.id},
        "email": ${databaseUser.email},
        "firstName": ${databaseUser.firstName},
        "lastName": ${databaseUser.lastName}
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonBody)
    response must haveStatus(Status.Ok)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "return a forbidden error response when the authenticated user does not have READ permissions for the resource user" in {
    val authenticatedDatabaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(authenticatedDatabaseUser.id)

    val resourceUser = RandomGenerator.databaseUser()

    val application =
      HttpTestApp[IO]()
        .withUser(authenticatedDatabaseUser)
        .withUser(resourceUser)
        .withAuthenticationToken(databaseAuthenticationToken)

    val request =
      authenticatedRequest[IO](databaseAuthenticationToken.secret, getRequest(`/v1` + `/user` + `/` + resourceUser.id))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonBody =
      json"""{
        "errorMessages": [ ${s"READ permissions not found for ${resourceUser.id}"} ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonBody)
    response must haveStatus(Status.Forbidden)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  s"POST ${`/v1` + `/user` + `/` + ":userId" + `/` + `weight-entry`}" should "successfully create a weight entry" in {
    val databaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val uuid = RandomGenerator.uuid()
    implicit val randomUuid: Random[IO, UUID] = random(uuid)

    val application = HttpTestApp[IO]().withUser(databaseUser).withAuthenticationToken(databaseAuthenticationToken)

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
      jsonRequest(Method.POST, `/v1` + `/user` + `/` + databaseUser.id + `/` + `weight-entry`, requestBody)
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

    response must beJsonContentType
    response must haveJson(expectedJsonBody)
    response must haveStatus(Status.Created)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  s"GET ${`/v1` + `/user` + `/` + ":userId" + `/` + `weight-entry` + "?" + PageNumberQueryParameter.key.value + "=Integer&" + PageSizeQueryParameter.key.value + "=Integer"}" should "return persisted weight entries" in {
    val databaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val weightEntryOne = RandomGenerator.databaseWeightEntry(databaseUser.id)
    val weightEntryTwo = RandomGenerator.databaseWeightEntry(databaseUser.id)
    val weightEntryThree = RandomGenerator.databaseWeightEntry(databaseUser.id)

    val application =
      HttpTestApp[IO]()
        .withUser(databaseUser)
        .withAuthenticationToken(databaseAuthenticationToken)
        .withWeightEntries(weightEntryOne, weightEntryTwo, weightEntryThree)

    val firstPageRequest = authenticatedRequest[IO](
      databaseAuthenticationToken.secret,
      Request[IO](uri = Uri.unsafeFromString(`/v1` + `/user` + `/` + databaseUser.id + `/` + `weight-entry` + "?" + PageSizeQueryParameter.key.value + "=2"))
        .putHeaders(`X-Correlation-ID`.from(RandomGenerator.uuid().toString))
    )

    val firstPageResponse = application.httpApp.run(firstPageRequest).unsafeRunSync()

    val firstPageExpectedJsonBody =
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

    firstPageResponse must beJsonContentType
    firstPageResponse must haveJson(firstPageExpectedJsonBody)
    firstPageResponse must haveStatus(Status.Ok)
    firstPageResponse must haveCorrelationIdOf(firstPageRequest)

    val secondPageRequest =
      authenticatedRequest(
        databaseAuthenticationToken.secret,
        Request[IO](
          uri = Uri.unsafeFromString(`/v1` + `/user` + `/` + databaseUser.id + `/` + `weight-entry` + "?" + PageNumberQueryParameter.key.value + "=1&" + PageSizeQueryParameter.key.value + "=2")
        ).putHeaders(`X-Correlation-ID`.from(RandomGenerator.uuid().toString))
      )

    val secondPageResponse =
      application.httpApp.run(secondPageRequest).unsafeRunSync()

    val secondPageExpectedJsonBody =
      json"""{
        "results": [
          {
            "id": ${weightEntryThree.id},
            "userId": ${weightEntryThree.userId},
            "timestamp": ${weightEntryThree.timestamp},
            "weight": ${weightEntryThree.weight},
            "description": ${weightEntryThree.description}
          }
        ],
        "pageNumber": 1,
        "pageSize": 2
      }"""

    secondPageResponse must beJsonContentType
    secondPageResponse must haveJson(secondPageExpectedJsonBody)
    secondPageResponse must haveStatus(Status.Ok)
    secondPageResponse must haveCorrelationIdOf(secondPageRequest)

    application.shutdown()
  }

  s"PUT ${`/v1` + `/user` + `/` + ":userId" + `/` + `reset-password`}" should "reset the user's password" in {
    val databaseUser = RandomGenerator.databaseUser()

    val secret = "this_is_a_secret"
    val databaseResetPasswordToken = DatabaseResetPasswordToken(
      databaseUser.id,
      secret,
      DateTime.now(),
      DateTime.now().plus(Duration.standardMinutes(1)),
      None
    )

    val newPassword = RandomGenerator.password()

    val application: HttpTestApp[IO] =
      HttpTestApp[IO]().withUser(databaseUser).withResetPasswordToken(databaseResetPasswordToken)

    val requestBody =
      json"""{
        "secret": $secret,
        "password": $newPassword
      }"""

    val request: Request[IO] =
      jsonRequest[IO](Method.PUT, `/v1`+ `/user` + `/` + databaseUser.id + `/` + `reset-password`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse: Json =
      json"""{
        "id": ${databaseUser.id},
        "email": ${databaseUser.email},
        "firstName": ${databaseUser.firstName},
        "lastName": ${databaseUser.lastName}
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Ok)
    response must haveCorrelationIdOf(request)

    val loginWithOldPasswordRequestBody: Json =
      json"""{
        "email": ${databaseUser.email},
        "password": ${RandomGenerator.PASSWORD}
      }"""

    val loginWithOldPasswordRequest = jsonRequest[IO](Method.POST, `/v1` + `/session`, loginWithOldPasswordRequestBody)

    val loginWithOldPasswordResponse = application.httpApp.run(loginWithOldPasswordRequest).unsafeRunSync()

    loginWithOldPasswordResponse must beJsonContentType
    loginWithOldPasswordResponse must haveStatus(Status.Unauthorized)
    loginWithOldPasswordResponse must haveCorrelationIdOf(loginWithOldPasswordRequest)

    val loginWithNewPasswordRequestBody =
      json"""{
        "email": ${databaseUser.email},
        "password": $newPassword
      }"""

    val loginWithNewPasswordRequest = jsonRequest[IO](Method.POST, `/v1` + `/session`, loginWithNewPasswordRequestBody)

    val loginWithNewPasswordResponse = application.httpApp.run(loginWithNewPasswordRequest).unsafeRunSync()

    loginWithNewPasswordResponse must beJsonContentType
    loginWithNewPasswordResponse must haveStatus(Status.Created)
    loginWithNewPasswordResponse must haveCorrelationIdOf(loginWithNewPasswordRequest)

    application.shutdown()
  }

  it should "return a not found error response for an invalid secret" in {
    val databaseUser = RandomGenerator.databaseUser()

    val application: HttpTestApp[IO] = HttpTestApp[IO]().withUser(databaseUser)

    val requestBody: Json =
      json"""{
        "secret": "non_existing_secret",
        "password": ${RandomGenerator.password()}
      }"""

    val request: Request[IO] =
      jsonRequest[IO](Method.PUT, `/v1` + `/user` + `/` + databaseUser.id + `/` + `reset-password`, requestBody)

    val response: Response[IO] = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Reset password token not found" ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.NotFound)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "return an authentication error response if the token is expired" in {
    val databaseUser = RandomGenerator.databaseUser()

    val secret = "this_is_awesome"
    val databaseResetPasswordToken =
      DatabaseResetPasswordToken(databaseUser.id, secret, DateTime.now(), DateTime.now(), None)

    implicit val clock: Clock[IO] =
      Providers.stubClock[IO](DateTime.now().plus(Duration.standardDays(10)))

    val application = HttpTestApp[IO]().withUser(databaseUser).withResetPasswordToken(databaseResetPasswordToken)

    val requestJsonBody =
      json"""{
        "secret": $secret,
        "password": ${RandomGenerator.password()}
      }"""

    val request = jsonRequest[IO](Method.PUT, `/v1`+ `/user` + `/` + databaseUser.id + `/` + `reset-password`, requestJsonBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Token is expired" ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Unauthorized)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "return an error response if the secret was already used to reset the password" in {
    val databaseUser = RandomGenerator.databaseUser()
    val secret = "awesome-secret"
    val databaseResetPasswordToken = DatabaseResetPasswordToken(
      databaseUser.id,
      secret,
      DateTime.now(),
      DateTime.now().plus(Duration.standardDays(1)),
      Some(DateTime.now().plus(Duration.standardHours(1)))
    )

    val application = HttpTestApp[IO]().withUser(databaseUser).withResetPasswordToken(databaseResetPasswordToken)

    val jsonRequestBody =
      json"""{
        "secret": $secret,
        "password": ${RandomGenerator.password()}
      }"""

    val request = jsonRequest[IO](Method.PUT, `/v1` + `/user` + `/` + databaseUser.id + `/` + `reset-password`, jsonRequestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Token has already been used" ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Unauthorized)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }
}
