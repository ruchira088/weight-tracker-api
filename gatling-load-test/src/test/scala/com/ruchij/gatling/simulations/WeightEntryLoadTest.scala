package com.ruchij.gatling.simulations

import com.ruchij.gatling.utils.GatlingUtils._
import com.ruchij.test.utils.RandomGenerator
import com.ruchij.web.routes.Paths._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.language.postfixOps

class WeightEntryLoadTest extends Simulation {
  val weightEntryScenario =
    scenario("Creating multiple weight entries")
      .exec(createUser)
      .exec(loginUser)
      .repeat(40) {
        exec {
          _.setAll(
            "timestamp" -> DateTime.now().toString,
            "weight" -> RandomGenerator.weight().toString,
            "description" -> RandomGenerator.description()
          )
        }.exec {
          http("Create weight entry")
            .post(`/v1` + `/user` + `/` + "${userId}" + `/` + `weight-entry`)
            .header(HttpHeaderNames.Authorization, bearerToken)
            .body {
              StringBody {
                """{
                    "timestamp": "${timestamp}",
                    "weight": "${weight}",
                    "description": "${description}"
                  }"""
              }
            }
        }
      }

  setUp { weightEntryScenario.inject(constantUsersPerSec(1).during(5 seconds)) }
    .protocols(http.baseUrl("http://localhost:8000"))
}
