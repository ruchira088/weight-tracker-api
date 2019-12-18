package com.ruchij.gatling.simulations

import com.ruchij.gatling.utils.GatlingUtils._
import com.ruchij.web.routes.Paths._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._
import scala.language.postfixOps

class UserLoadTest extends Simulation {
  val httpProtocol: HttpProtocolBuilder =
    http.baseUrl("http://localhost:8000")

  val userScenario: ScenarioBuilder =
    scenario("Checking service health")
      .exec(createUser)
      .exec(loginUser)
      .exec {
        http("Authenticated user")
          .get(`/v1` + `/session` + `/` + `user`)
          .header(HttpHeaderNames.Authorization, bearerToken)
          .check(userCheck: _*)
      }
      .exec {
        http("Fetch user")
          .get(`/v1` + `/user` + `/` + "${userId}")
          .header(HttpHeaderNames.Authorization, bearerToken)
          .check(userCheck: _*)
      }
      .exec {
        http("Delete user")
          .delete(`/v1` + `/user` + `/` + "${userId}")
          .header(HttpHeaderNames.Authorization, bearerToken)
          .check(userCheck: _*)
      }

  setUp { userScenario.inject(constantUsersPerSec(10).during(20 seconds)) }
    .protocols(httpProtocol)
}
