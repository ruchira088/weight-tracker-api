package com.ruchij.web.routes

import cats.effect.{Clock, IO}
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.circe.Encoders.jodaTimeEncoder
import com.ruchij.test.TestHttpApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.JsonUtils.json
import com.ruchij.test.utils.Providers.{contextShift, stubClock}
import io.circe.literal._
import org.http4s.{Request, Response, Status, Uri}
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, MustMatchers}

import scala.util.Properties

class HealthRoutesSpec extends FlatSpec with MustMatchers {

  "GET /health" should "return a successful response containing service information" in {
    val currentDateTime = DateTime.now()
    implicit val clock: Clock[IO] = stubClock[IO](currentDateTime)

    val httpApp = TestHttpApp[IO]()

    val request = Request[IO](uri = Uri(path = "/health"))

    val response: Response[IO] =
      httpApp.run(request).unsafeRunSync()

    val expectedResponse =
      json"""{
         "serviceName": "weight-tracker-api",
         "serviceVersion": "0.0.1",
         "javaVersion": ${Properties.javaVersion},
         "sbtVersion": ${BuildInfo.sbtVersion},
         "scalaVersion": ${BuildInfo.scalaVersion},
         "currentTimestamp": $currentDateTime
      }"""

    response.status mustBe Status.Ok
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponse)
  }
}
