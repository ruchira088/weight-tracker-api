package com.ruchij.web.routes

import cats.effect.{Clock, IO}
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.services.health.HealthCheckServiceImpl
import com.ruchij.test.matchers._
import com.ruchij.test.stubs.{clock => stubClock }
import com.ruchij.test.utils.{JsonParser, json, responseEval}
import org.http4s.dsl.Http4sDsl
import org.http4s.{Request, Response, Status, Uri}
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, MustMatchers}

import scala.util.Properties

class HealthRoutesSpec extends FlatSpec with MustMatchers {

  "/health" should "return a successful response" in {
    val currentDateTime = DateTime.now()
    implicit val clock: Clock[IO] = stubClock[IO](currentDateTime)

    val healthService = new HealthCheckServiceImpl[IO]

    implicit val http4sDsl: Http4sDsl[IO] = new Http4sDsl[IO] {}

    val request = Request[IO](uri = Uri(path = "/"))

    val response: Response[IO] =
      responseEval(HealthRoutes(healthService), request).unsafeRunSync()

    val expectedResponse =
      s"""{
         "serviceName": "weight-tracker-api",
         "serviceVersion": "0.0.1",
         "javaVersion": "${Properties.javaVersion}",
         "sbtVersion": "${BuildInfo.sbtVersion}",
         "scalaVersion": "${BuildInfo.scalaVersion}",
         "currentTimestamp": "$currentDateTime"
      }""".parseAsJson[IO]

    response.status mustBe Status.Ok
    response must beJsonResponse[IO]
    json(response) must matchWith(expectedResponse)
  }
}
