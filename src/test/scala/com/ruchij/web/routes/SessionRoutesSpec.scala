package com.ruchij.web.routes

import java.util.UUID

import cats.effect.{Clock, IO}
import com.ruchij.circe.Encoders.{jodaTimeEncoder, taggedStringEncoder}
import com.ruchij.daos.authenticationfailure.models.DatabaseAuthenticationFailure
import com.ruchij.daos.lockeduser.models.DatabaseLockedUser
import com.ruchij.daos.resetpassword.models.DatabaseResetPasswordToken
import com.ruchij.services.authentication.models.ResetPasswordToken
import com.ruchij.services.email.models.Email
import com.ruchij.services.user.models.User
import com.ruchij.test.HttpTestApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.Providers.{clock, contextShift, random, stubClock}
import com.ruchij.test.utils.{Providers, RandomGenerator}
import com.ruchij.test.utils.RequestUtils.{authenticatedRequest, getRequest, jsonRequest}
import com.ruchij.types.Random
import com.ruchij.types.FunctionKTypes._
import com.ruchij.web.headers.`X-Correlation-ID`
import com.ruchij.web.routes.Paths.{`/`, `/session`, `/v1`, `reset-password`, `user`}
import io.circe.Json
import io.circe.literal._
import org.http4s.{Method, Request, Response, Status, Uri}
import org.http4s.circe.jsonEncoder
import org.joda.time.{DateTime, Duration}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues

class SessionRoutesSpec extends AnyFlatSpec with Matchers with OptionValues {

  s"POST ${`/v1` + `/session`}" should "successfully create an authentication token for valid credentials" in {
    val timestamp = DateTime.now()
    val uuid = RandomGenerator.uuid()

    implicit val clock: Clock[IO] = stubClock[IO](timestamp)
    implicit val randomUuid: Random[IO, UUID] = random[IO, UUID](uuid)

    val databaseUser = RandomGenerator.databaseUser()

    val application = HttpTestApp[IO]().withUser(databaseUser)

    val requestBody: Json =
      json"""{
        "email": ${databaseUser.email},
        "password": ${RandomGenerator.PASSWORD}
      }"""

    val request = jsonRequest[IO, Json](Method.POST, `/v1` + `/session`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse: Json =
      json"""{
        "userId": ${databaseUser.id},
        "expiresAt": ${timestamp.plus(HttpTestApp.SESSION_TIMEOUT.toMillis)},
        "secret": $uuid
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Created)
    response must haveCorrelationIdOf(request)

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

    val request = jsonRequest[IO, Json](Method.POST, `/v1` + `/session`, requestBody)

    val response: Response[IO] =
      application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse: Json =
      json"""{
        "errorMessages": [
          "Invalid credentials. 2 incorrect authentication attempts remain before the user account is locked"
        ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Unauthorized)
    response must haveCorrelationIdOf(request)

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

    val request = jsonRequest[IO, Json](Method.POST, `/v1` + `/session`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ ${s"Email not found: $email"} ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.NotFound)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "lock the user account if the number of incorrect login attempts exceed the maximum brute force count" in {
    val databaseUser = RandomGenerator.databaseUser()

    val application = HttpTestApp[IO]().withUser(databaseUser)

    val requestJsonBody =
      json"""{
        "email": ${databaseUser.email},
        "password": ${RandomGenerator.password()}
      }"""

    val request = jsonRequest[IO, Json](Method.POST, `/v1` + `/session`, requestJsonBody)

    val responseOne = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponseOne =
      json"""{
        "errorMessages": [
          "Invalid credentials. 2 incorrect authentication attempts remain before the user account is locked"
         ]
      }"""

    responseOne must beJsonContentType
    responseOne must haveJson(expectedJsonResponseOne)
    responseOne must haveStatus(Status.Unauthorized)
    responseOne must haveCorrelationIdOf(request)

    val responseTwo = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponseTwo =
      json"""{
        "errorMessages": [
          "Invalid credentials. 1 incorrect authentication attempts remain before the user account is locked"
        ]
      }"""

    responseTwo must beJsonContentType
    responseTwo must haveJson(expectedJsonResponseTwo)
    responseTwo must haveStatus(Status.Unauthorized)
    responseTwo must haveCorrelationIdOf(request)

    val responseThree = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponseThree =
      json"""{
        "errorMessages": [
          "User account has been locked due to 3 incorrect authentication attempts"
        ]
      }"""

    responseThree must beJsonContentType
    responseThree must haveJson(expectedJsonResponseThree)
    responseThree must haveStatus(Status.Unauthorized)
    responseThree must haveCorrelationIdOf(request)

    val responseFour = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponseFour =
      json"""{
        "errorMessages": [ "User account is locked" ]
      }"""

    responseFour must beJsonContentType
    responseFour must haveJson(expectedJsonResponseFour)
    responseFour must haveStatus(Status.Unauthorized)
    responseFour must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "disregard the failed authentication attempts, outside the roll over period" in {
    val databaseUser = RandomGenerator.databaseUser()

    val authenticationFailureOne =
      DatabaseAuthenticationFailure(
        RandomGenerator.uuid(),
        databaseUser.id,
        DateTime.now().minusSeconds(50),
        deleted = false
      )
    val authenticationFailureTwo =
      DatabaseAuthenticationFailure(RandomGenerator.uuid(), databaseUser.id, DateTime.now(), deleted = false)

    val application = HttpTestApp[IO]()
      .withUser(databaseUser)
      .withAuthenticationFailure(authenticationFailureOne)
      .withAuthenticationFailure(authenticationFailureTwo)

    val jsonRequestBody =
      json"""{
        "email": ${databaseUser.email},
        "password": ${RandomGenerator.password()}
      }"""

    val request = jsonRequest[IO, Json](Method.POST, `/v1` + `/session`, jsonRequestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [
          "Invalid credentials. 1 incorrect authentication attempts remain before the user account is locked"
        ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Unauthorized)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "prevent user logins, if the user account is locked" in {
    val databaseUser = RandomGenerator.databaseUser()
    val lockedUser = DatabaseLockedUser(databaseUser.id, DateTime.now(), "unlock-code", None)

    val application = HttpTestApp[IO]().withUser(databaseUser).withLockedUser(lockedUser)

    val jsonRequestBody =
      json"""{
        "email": ${databaseUser.email},
        "password": ${RandomGenerator.PASSWORD}
      }"""

    val request = jsonRequest[IO, Json](Method.POST, `/v1` + `/session`, jsonRequestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [
          "User account is locked"
        ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Unauthorized)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  s"GET ${`/v1` + `/session` + `/` + `user`}" should "return the authenticated user" in {
    val databaseUser = RandomGenerator.databaseUser()
    val databaseAuthenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val application: HttpTestApp[IO] =
      HttpTestApp[IO]().withUser(databaseUser).withAuthenticationToken(databaseAuthenticationToken)

    val request =
      authenticatedRequest(databaseAuthenticationToken.secret, getRequest[IO](`/v1` + `/session` + `/` + `user`))

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

    application.shutdown()
  }

  it should "return unauthorized error response when the authentication token is expired" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    implicit val clock: Clock[IO] = Providers.stubClock[IO](DateTime.now().plus(Duration.standardDays(1)))

    val application = HttpTestApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val request = authenticatedRequest[IO](authenticationToken.secret, getRequest(`/v1` + `/session` + `/` + `user`))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonBody =
      json"""{
        "errorMessages": [ "Expired credentials" ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonBody)
    response must haveStatus(Status.Unauthorized)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "return unauthorized error response when the Authorization header is missing" in {
    val databaseUser = RandomGenerator.databaseUser()

    val application = HttpTestApp[IO]().withUser(databaseUser)

    val request = getRequest[IO](`/v1` + `/session` + `/` + `user`)

    val response: Response[IO] = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ "Missing Authorization header" ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Unauthorized)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  it should "return an unauthorized error response if the user account is locked" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)
    val lockedUser = DatabaseLockedUser(databaseUser.id, DateTime.now(), "unlock-code", None)

    val application =
      HttpTestApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken).withLockedUser(lockedUser)

    val request = authenticatedRequest(authenticationToken.secret, getRequest[IO](`/v1` + `/session` + `/` + `user`))

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [
          "User account is locked"
        ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Unauthorized)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  s"DELETE ${`/v1` + `/session`}" should "successfully remove the authentication token" in {
    val databaseUser = RandomGenerator.databaseUser()
    val authenticationToken = RandomGenerator.databaseAuthenticationToken(databaseUser.id)

    val timestamp = DateTime.now()
    implicit val clock: Clock[IO] = stubClock(timestamp)

    val application = HttpTestApp[IO]().withUser(databaseUser).withAuthenticationToken(authenticationToken)

    val retrieveUserRequest =
      authenticatedRequest[IO](authenticationToken.secret, getRequest(`/v1` + `/session` + `/` + `user`))

    val retrieveUserResponse = application.httpApp.run(retrieveUserRequest).unsafeRunSync()

    retrieveUserResponse must beJsonContentType
    retrieveUserResponse must haveStatus(Status.Ok)
    retrieveUserResponse must haveCorrelationIdOf(retrieveUserRequest)

    val logoutRequest =
      authenticatedRequest[IO](
        authenticationToken.secret,
        Request(Method.DELETE, Uri(path = `/v1` + `/session`))
          .putHeaders(`X-Correlation-ID`.from(RandomGenerator.uuid().toString))
      )

    val logoutResponse = application.httpApp.run(logoutRequest).unsafeRunSync()

    val expectedLogoutJsonBody =
      json"""{
        "userId": ${authenticationToken.userId},
        "expiresAt": ${timestamp.plus(HttpTestApp.SESSION_TIMEOUT.toMillis)},
        "secret": ${authenticationToken.secret}
      }"""

    logoutResponse must beJsonContentType
    logoutResponse must haveJson(expectedLogoutJsonBody)
    logoutResponse must haveStatus(Status.Ok)
    logoutResponse must haveCorrelationIdOf(logoutRequest)

    val retrieveUserAgainResponse = application.httpApp.run(retrieveUserRequest).unsafeRunSync()

    val expectedFailureJsonBody =
      json"""{
        "errorMessages": [ "Invalid credentials" ]
      }"""

    retrieveUserAgainResponse must beJsonContentType
    retrieveUserAgainResponse must haveJson(expectedFailureJsonBody)
    retrieveUserAgainResponse must haveStatus(Status.Unauthorized)
    retrieveUserAgainResponse must haveCorrelationIdOf(retrieveUserRequest)

    application.shutdown()
  }

  s"POST ${`/v1` + `/session` + `/` + `reset-password`}" should "create a password reset token and send a password reset email" in {
    val currentDateTime = DateTime.now()
    implicit val clock: Clock[IO] = Providers.stubClock[IO](currentDateTime)

    val uuid = RandomGenerator.uuid()
    implicit val randomUuid: Random[IO, UUID] = random(uuid)

    val databaseUser = RandomGenerator.databaseUser()
    val frontEndUrl = RandomGenerator.url()

    val requestBody =
      json"""{
        "email": ${databaseUser.email},
        "frontEndUrl": $frontEndUrl
      }"""

    val expiresAt = currentDateTime.plus(HttpTestApp.SESSION_TIMEOUT.toMillis)

    val application = HttpTestApp[IO]().withUser(databaseUser)

    val request = jsonRequest[IO, Json](Method.POST, `/v1` + `/session` + `/` + `reset-password`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedResponseBody =
      json"""{
        "email": ${databaseUser.email},
        "expiresAt": $expiresAt,
        "frontEndUrl": $frontEndUrl
      }"""

    response must beJsonContentType
    response must haveJson(expectedResponseBody)
    response must haveStatus(Status.Created)
    response must haveCorrelationIdOf(request)

    application.externalEmailMailBox.size mustBe 1
    application.externalEmailMailBox.peek mustBe
      Email.resetPassword(
        User.fromDatabaseUser(databaseUser),
        ResetPasswordToken(databaseUser.id, uuid.toString, expiresAt, used = false),
        frontEndUrl
      )

    val expectedDatabasePasswordResetToken =
      DatabaseResetPasswordToken(databaseUser.id, uuid.toString, currentDateTime, expiresAt, None)

    val databaseResetPasswordToken: DatabaseResetPasswordToken =
      application.resetPasswordTokenDao.find(databaseUser.id, uuid.toString).value.unsafeRunSync().value

    databaseResetPasswordToken mustBe expectedDatabasePasswordResetToken

    application.shutdown()
  }

  it should "return a not found error response when the email does not exist" in {

    val email = RandomGenerator.email()
    val frontEndUrl = RandomGenerator.url()

    val requestBody: Json =
      json"""{
        "email": ${email.toString},
        "frontEndUrl": $frontEndUrl
      }"""

    val application: HttpTestApp[IO] = HttpTestApp[IO]()

    val request = jsonRequest[IO, Json](Method.POST, `/v1` + `/session` + `/` + `reset-password`, requestBody)

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "errorMessages": [ ${email.toString + " was not found"} ]
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.NotFound)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }
}
