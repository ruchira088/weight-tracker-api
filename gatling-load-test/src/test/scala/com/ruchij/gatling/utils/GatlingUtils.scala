package com.ruchij.gatling.utils

import com.ruchij.test.utils.RandomGenerator
import com.ruchij.web.routes.Paths.{`/session`, `/user`, `/v1`}
import io.gatling.core.Predef._
import io.gatling.core.feeder.FeederBuilder
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.check.HttpCheck

object GatlingUtils {

  val bearerToken = "Bearer ${secret}"

  val feeder: FeederBuilder =
    () =>
      Iterator.continually {
        Map[String, String](
          "email" -> RandomGenerator.email(),
          "firstName" -> RandomGenerator.firstName(),
          "lastName" -> RandomGenerator.lastName(),
          "password" -> RandomGenerator.password()
        )
    }

  val createUser: ChainBuilder =
    feed(feeder)
      .exec {
        http("Create User")
          .post(`/v1` + `/user`)
          .body {
            StringBody {
              """{
                 "email": "${email}",
                 "password": "${password}",
                 "firstName": "${firstName}",
                 "lastName": "${lastName}"
              }"""
            }
          }
          .asJson
          .check(jsonPath("$..id").saveAs("userId"))
      }

  val loginUser: ChainBuilder =
    exec {
      http("Login")
        .post(`/v1` + `/session`)
        .body {
          StringBody {
            """{
              "email": "${email}",
              "password": "${password}"
            }"""
          }
        }
        .asJson
        .check(jsonPath("$..secret").saveAs("secret"))
    }

  val userCheck: List[HttpCheck] =
    List(
      jsonPath("$..id").is("${userId}"),
      jsonPath("$..email").is("${email}"),
      jsonPath("$..firstName").is("${firstName}"),
      jsonPath("$..lastName").is("${lastName}")
    )
}
