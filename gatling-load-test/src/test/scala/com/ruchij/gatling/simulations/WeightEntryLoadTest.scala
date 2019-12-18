package com.ruchij.gatling.simulations

import cats.effect.IO
import com.ruchij.gatling.config.LoadTestConfiguration
import com.ruchij.gatling.utils.GatlingUtils._
import com.ruchij.test.utils.RandomGenerator
import com.ruchij.types.FunctionKTypes.eitherThrowableToIO
import com.ruchij.web.routes.Paths._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import org.joda.time.DateTime
import pureconfig.ConfigSource

import scala.concurrent.duration._
import scala.language.postfixOps

class WeightEntryLoadTest extends Simulation {

  val weightEntryScenario: ScenarioBuilder =
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

  LoadTestConfiguration
    .loadF[IO](ConfigSource.resources("application.load-test.conf"))
    .map { loadTestConfiguration =>
      setUp { weightEntryScenario.inject(constantUsersPerSec(1).during(5 seconds)) }
        .protocols(http.baseUrl(loadTestConfiguration.baseUrl))
    }
    .unsafeRunSync()
}
