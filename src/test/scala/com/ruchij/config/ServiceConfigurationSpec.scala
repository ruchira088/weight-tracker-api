package com.ruchij.config

import com.ruchij.config.AuthenticationConfiguration.BruteForceProtectionConfiguration
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import pureconfig.ConfigSource

import scala.concurrent.duration._
import scala.language.postfixOps

class ServiceConfigurationSpec extends AnyFlatSpec with Matchers {

  "Loading the ServiceConfiguration" should "return a successful ServiceConfiguration for a configuration file" in {
    val currentDateTime = DateTime.now().withZone(DateTimeZone.UTC)

    val configurationFile =
      s"""
         |http-configuration {
         |  port = 80
         |}
         |
         |doobie-configuration {
         |  driver = "ruchij.com"
         |  url = "jdbc://ruchij.com/awesome_db"
         |  user = "john.doe"
         |  password = "my-password"
         |}
         |
         |authentication-configuration {
         |  session-timeout = "60s"
         |
         |  brute-force-protection {
         |    maximum-failures = 10
         |    roll-over-period = "30s"
         |  }
         |}
         |
         |redis-configuration {
         |  host = "redis-server"
         |  port = 6379
         |  password = "password"
         |}
         |
         |email-configuration {
         |  sendgrid-api-key = "secret-sendgrid-key"
         |}
         |
         |build-information {
         |  git-branch = "master"
         |  git-commit = "1234abc"
         |  build-timestamp = "$currentDateTime"
         |}
         |""".stripMargin

    val expectedServiceConfiguration =
      ServiceConfiguration(
        HttpConfiguration(port = 80),
        DoobieConfiguration(
          driver = "ruchij.com",
          url = "jdbc://ruchij.com/awesome_db",
          user = "john.doe",
          password = "my-password"
        ),
        AuthenticationConfiguration(60 seconds, BruteForceProtectionConfiguration(10, 30 seconds)),
        RedisConfiguration(host = "redis-server", port = 6379, password = Some("password")),
        EmailConfiguration("secret-sendgrid-key"),
        BuildInformation(Some("master"), Some("1234abc"), Some(currentDateTime))
      )

    ServiceConfiguration.load(ConfigSource.string(configurationFile)) mustBe Right(expectedServiceConfiguration)
  }
}
