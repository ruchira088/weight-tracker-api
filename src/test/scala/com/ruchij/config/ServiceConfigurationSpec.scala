package com.ruchij.config

import java.util.concurrent.TimeUnit

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{FlatSpec, MustMatchers}
import pureconfig.ConfigSource

import scala.concurrent.duration.FiniteDuration

class ServiceConfigurationSpec extends FlatSpec with MustMatchers {

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
         |  session-timeout = 60s
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
        AuthenticationConfiguration(FiniteDuration(60, TimeUnit.SECONDS)),
        RedisConfiguration(host = "redis-server", port = 6379, password = Some("password")),
        EmailConfiguration("secret-sendgrid-key"),
        BuildInformation(Some("master"), Some("1234abc"), Some(currentDateTime))
      )

    ServiceConfiguration.load(ConfigSource.string(configurationFile)) mustBe Right(expectedServiceConfiguration)
  }
}
