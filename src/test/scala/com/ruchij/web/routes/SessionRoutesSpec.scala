package com.ruchij.web.routes

import java.util.UUID

import cats.effect.{Clock, IO}
import com.ruchij.circe.Encoders.{jodaTimeEncoder, taggedStringEncoder}
import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import com.ruchij.services.authentication.models.ResetPasswordToken
import com.ruchij.services.email.models.Email
import com.ruchij.services.user.models.User
import com.ruchij.test.TestHttpApp
import com.ruchij.test.matchers.{beJsonResponse, matchWith}
import com.ruchij.test.stubs.FlexibleClock
import com.ruchij.test.utils.JsonUtils.json
import com.ruchij.test.utils.Providers.{clock, contextShift, stubClock}
import com.ruchij.test.utils.{Providers, RandomGenerator}
import com.ruchij.test.utils.RequestUtils.{authenticatedRequest, getRequest, jsonRequest}
import com.ruchij.types.Random
import com.ruchij.web.routes.Paths.{`/session`, `reset-password`, user}
import io.circe.Json
import io.circe.literal._
import org.http4s.{Method, Request, Response, Status, Uri}
import org.joda.time.{DateTime, Duration}
import org.scalatest.{FlatSpec, MustMatchers, OptionValues}

class SessionRoutesSpec extends FlatSpec with MustMatchers with OptionValues {

  "/POST session" should "successfully create an authentication token for valid credentials" in {
    val databaseUser = RandomGenerator.databaseUser()

    val application = TestHttpApp[IO]().withUser(databaseUser)

    val requestBody: Json =
      json"""{
        "email": ${databaseUser.email},
        "password": ${RandomGenerator.PASSWORD}
      }"""

    val response = application.httpApp.run(jsonRequest(Method.POST, `/session`, requestBody)).unsafeRunSync()

    response must beJsonResponse[IO]
    response.status mustBe Status.Created

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

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)
    response.status mustBe Status.Unauthorized

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

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)
    response.status mustBe Status.NotFound

    application.shutdown()
  }

  "GET /session/user" should "return the authenticated user" in {
    val databaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val application: TestHttpApp[IO] =
      TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(databaseAuthenticationToken)

    val request = authenticatedRequest(databaseAuthenticationToken.secret, getRequest[IO](s"${`/session`}/$user"))

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

  it should "return unauthorized error response when the authentication token is expired" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    implicit val flexibleClock: FlexibleClock[IO] = new FlexibleClock[IO](DateTime.now())

    val application = TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    flexibleClock.setDateTime(DateTime.now().plusDays(1))

    val request = authenticatedRequest[IO](authenticationToken.secret, getRequest(s"${`/session`}/$user"))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonBody =
      json"""{
        "errorMessages": [ "Expired authentication token" ]
      }"""

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonBody)
    response.status mustBe Status.Unauthorized

    application.shutdown()
  }

  it should "return unauthorized error response when the Authorization header is missing" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val application = TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val response: Response[IO] = application.httpApp.run(getRequest[IO](s"${`/session`}/$user")).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Missing Authorization header" ]
      }"""

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)
    response.status mustBe Status.Unauthorized

    application.shutdown()
  }

  "DELETE /session" should "successfully remove the authentication token" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val timestamp = DateTime.now()
    implicit val clock: Clock[IO] = stubClock(timestamp)

    val application = TestHttpApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val retrieveUserRequest =
      authenticatedRequest[IO](authenticationToken.secret, getRequest(s"${`/session`}/$user"))

    val retrieveUserResponse = application.httpApp.run(retrieveUserRequest).unsafeRunSync()

    retrieveUserResponse.status mustBe Status.Ok

    val logoutRequest =
      authenticatedRequest[IO](authenticationToken.secret, Request(Method.DELETE, Uri(path = `/session`)))

    val logoutResponse = application.httpApp.run(logoutRequest).unsafeRunSync()

    val expectedLogoutJsonBody =
      json"""{
        "userId": ${authenticationToken.userId},
        "expiresAt": ${timestamp.plusSeconds(30)},
        "secret": ${authenticationToken.secret}
      }"""

    logoutResponse must beJsonResponse[IO]
    json(logoutResponse) must matchWith(expectedLogoutJsonBody)
    logoutResponse.status mustBe Status.Ok

    val retrieveUserAgainResponse = application.httpApp.run(retrieveUserRequest).unsafeRunSync()

    val expectedFailureJsonBody =
      json"""{
        "errorMessages": [ "Invalid credentials" ]
      }"""

    retrieveUserAgainResponse must beJsonResponse[IO]
    json(retrieveUserAgainResponse) must matchWith(expectedFailureJsonBody)
    retrieveUserAgainResponse.status mustBe Status.Unauthorized

    application.shutdown()
  }

  "POST /session/reset-password" should "create a password reset token and send a password reset email" in {
    val currentDateTime = DateTime.now()
    implicit val clock: Clock[IO] = Providers.stubClock[IO](currentDateTime)

    val uuid = RandomGenerator.uuid()
    implicit val randomUuid: Random[IO, UUID] = RandomGenerator.random(uuid)

    val databaseUser = RandomGenerator.databaseUser()

    val requestBody =
      json"""{
        "email": ${databaseUser.email}
      }"""

    val expiresAt = currentDateTime.plus(Duration.standardSeconds(30))

    val application = TestHttpApp[IO]().withUser(databaseUser)

    val request = jsonRequest[IO](Method.POST, s"${`/session`}/${`reset-password`}", requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedResponseBody =
      json"""{
        "email": ${databaseUser.email},
        "expiresAt": $expiresAt
      }"""

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponseBody)
    response.status mustBe Status.Created

    application.externalEmailMailBox.size mustBe 1
    application.externalEmailMailBox.peek mustBe
      Email.resetPassword(User.fromDatabaseUser(databaseUser), ResetPasswordToken(databaseUser.id, uuid.toString, expiresAt, used = false))

    val expectedDatabasePasswordResetToken =
      DatabaseResetPasswordToken(databaseUser.id, uuid.toString, currentDateTime, expiresAt, None)

    val databaseResetPasswordToken: DatabaseResetPasswordToken =
      application.resetPasswordTokenDao.find(databaseUser.id, uuid.toString).value.unsafeRunSync().value

    databaseResetPasswordToken mustBe expectedDatabasePasswordResetToken

    application.shutdown()
  }
}
