package com.ruchij.web.routes

import cats.effect.{Clock, IO}
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.circe.Encoders.jodaTimeEncoder
import com.ruchij.test.TestHttpApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.JsonUtils.json
import com.ruchij.test.utils.RequestUtils.getRequest
import com.ruchij.test.utils.Providers.{contextShift, stubClock}
import com.ruchij.web.routes.Paths.`/health`
import io.circe.literal._
import org.http4s.{Response, Status}
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, MustMatchers}

import scala.util.Properties

class HealthRoutesSpec extends FlatSpec with MustMatchers {

  "GET /health" should "return a successful response containing service information" in {
    val currentDateTime = DateTime.now()
    implicit val clock: Clock[IO] = stubClock[IO](currentDateTime)

    val application: TestHttpApp[IO] = TestHttpApp[IO]()

    val request = getRequest[IO](`/health`)

    val response: Response[IO] =
      application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
         "serviceName": "weight-tracker-api",
         "serviceVersion": "0.0.1",
         "javaVersion": ${Properties.javaVersion},
         "sbtVersion": ${BuildInfo.sbtVersion},
         "scalaVersion": ${BuildInfo.scalaVersion},
         "currentTimestamp": $currentDateTime,
         "gitBranch": "master",
         "gitCommit": "abc1234",
         "buildTimestamp": null
      }"""

    response must beJsonResponse[IO]
    json(response) must matchWith(expectedJsonResponse)
    response.status mustBe Status.Ok

    application.shutdown()
  }
}
