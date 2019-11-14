package com.ruchij.web.routes

import java.util.UUID

import cats.effect.{Clock, IO}
import com.ruchij.circe.Encoders.{jodaTimeEncoder, taggedStringEncoder}
import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import com.ruchij.services.authentication.models.ResetPasswordToken
import com.ruchij.services.email.models.Email
import com.ruchij.services.user.models.User
import com.ruchij.test.HttpTestApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.Providers.{clock, contextShift, stubClock}
import com.ruchij.test.utils.{Providers, RandomGenerator}
import com.ruchij.test.utils.RequestUtils.{authenticatedRequest, getRequest, jsonRequest}
import com.ruchij.types.Random
import com.ruchij.types.FunctionKTypes._
import com.ruchij.web.routes.Paths.{`/session`, `reset-password`, user}
import io.circe.Json
import io.circe.literal._
import org.http4s.{Method, Request, Response, Status, Uri}
import org.joda.time.{DateTime, Duration}
import org.scalatest.{FlatSpec, MustMatchers, OptionValues}

class SessionRoutesSpec extends FlatSpec with MustMatchers with OptionValues {

  "POST /session" should "successfully create an authentication token for valid credentials" in {
    val databaseUser = RandomGenerator.databaseUser()

    val application = HttpTestApp[IO]().withUser(databaseUser)

    val requestBody: Json =
      json"""{
        "email": ${databaseUser.email},
        "password": ${RandomGenerator.PASSWORD}
      }"""

    val response = application.httpApp.run(jsonRequest(Method.POST, `/session`, requestBody)).unsafeRunSync()

    response must beJsonContentType
    response must haveStatus(Status.Created)

    application.shutdown()
  }

  it should "return an authorized error response for an incorrect password" in {
    val databaseUser = RandomGenerator.databaseUser()

    val application: HttpTestApp[IO] = HttpTestApp[IO]().withUser(databaseUser)

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

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Unauthorized)

    application.shutdown()
  }

  it should "return not found response (404) when the email doesn't exist" in {
    val application: HttpTestApp[IO] = HttpTestApp[IO]()

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

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.NotFound)

    application.shutdown()
  }

  "GET /session/user" should "return the authenticated user" in {
    val databaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val application: HttpTestApp[IO] =
      HttpTestApp[IO]().withUser(databaseUser).withAuthenticationToken(databaseAuthenticationToken)

    val request = authenticatedRequest(databaseAuthenticationToken.secret, getRequest[IO](s"${`/session`}/$user"))

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

    application.shutdown()
  }

  it should "return unauthorized error response when the authentication token is expired" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    implicit val clock: Clock[IO] = Providers.stubClock[IO](DateTime.now().plus(Duration.standardDays(1)))

    val application = HttpTestApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val request = authenticatedRequest[IO](authenticationToken.secret, getRequest(s"${`/session`}/$user"))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonBody =
      json"""{
        "errorMessages": [ "Expired credentials" ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonBody)
    response must haveStatus(Status.Unauthorized)

    application.shutdown()
  }

  it should "return unauthorized error response when the Authorization header is missing" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val application = HttpTestApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val response: Response[IO] = application.httpApp.run(getRequest[IO](s"${`/session`}/$user")).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Missing Authorization header" ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Unauthorized)

    application.shutdown()
  }

  "DELETE /session" should "successfully remove the authentication token" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val timestamp = DateTime.now()
    implicit val clock: Clock[IO] = stubClock(timestamp)

    val application = HttpTestApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

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

    logoutResponse must beJsonContentType
    logoutResponse must haveJson(expectedLogoutJsonBody)
    logoutResponse must haveStatus(Status.Ok)

    val retrieveUserAgainResponse = application.httpApp.run(retrieveUserRequest).unsafeRunSync()

    val expectedFailureJsonBody =
      json"""{
        "errorMessages": [ "Invalid credentials" ]
      }"""

    retrieveUserAgainResponse must beJsonContentType
    retrieveUserAgainResponse must haveJson(expectedFailureJsonBody)
    retrieveUserAgainResponse must haveStatus(Status.Unauthorized)

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

    val application = HttpTestApp[IO]().withUser(databaseUser)

    val request = jsonRequest[IO](Method.POST, s"${`/session`}/${`reset-password`}", requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedResponseBody =
      json"""{
        "email": ${databaseUser.email},
        "expiresAt": $expiresAt
      }"""

    response must beJsonContentType
    response must haveJson(expectedResponseBody)
    response must haveStatus(Status.Created)

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

  it should "return a not found error response when the email does not exist" in {

    val email = RandomGenerator.email()

    val requestBody: Json =
      json"""{
        "email": ${email.toString}
      }"""

    val application: HttpTestApp[IO] = HttpTestApp[IO]()

    val request = jsonRequest[IO](Method.POST, s"${`/session`}/${`reset-password`}", requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ ${email.toString + " was not found"} ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.NotFound)

    application.shutdown()
  }
}
