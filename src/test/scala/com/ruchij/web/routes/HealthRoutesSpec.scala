package com.ruchij.web.routes

import cats.effect.{Clock, IO}
import com.ruchij.circe.Encoders.jodaTimeCirceEncoder
import com.ruchij.test.HttpTestApp
import com.ruchij.test.matchers._
import com.ruchij.test.utils.RequestUtils.getRequest
import com.ruchij.test.utils.Providers.{contextShift, stubClock, clock}
import com.ruchij.types.FunctionKTypes._
import com.ruchij.web.routes.Paths.{`/health`, `services`}
import io.circe.literal._
import org.http4s.{Response, Status}
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.util.Properties

class HealthRoutesSpec extends AnyFlatSpec with Matchers {

  s"GET ${`/health`}" should "return a successful response containing service information" in {
    val currentDateTime = DateTime.now()
    implicit val clock: Clock[IO] = stubClock[IO](currentDateTime)

    val application: HttpTestApp[IO] = HttpTestApp[IO]()

    val request = getRequest[IO](`/health`)

    val response: Response[IO] = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
         "applicationMode": "Local",
         "serviceName": "weight-tracker-api",
         "serviceVersion": "0.0.1",
         "organization": "com.ruchij",
         "javaVersion": ${Properties.javaVersion},
         "sbtVersion": "1.3.6",
         "scalaVersion": "2.12.10",
         "currentTimestamp": $currentDateTime,
         "gitBranch": "master",
         "gitCommit": "abc1234",
         "buildTimestamp": null
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Ok)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }

  s"GET ${`/health`}/${`services`}" should "return a success response when all services are healthy" in {
    val application = HttpTestApp[IO]()

    val request = getRequest[IO](s"${`/health`}/${`services`}")

    val response = application.httpApp.run(request).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "database": "Healthy",
        "redis": "Healthy",
        "publisher": "Healthy"
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Ok)
    response must haveCorrelationIdOf(request)

    application.shutdown()
  }
}
