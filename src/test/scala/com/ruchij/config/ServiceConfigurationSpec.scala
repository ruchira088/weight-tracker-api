package com.ruchij.config

import cats.effect.IO
import com.ruchij.config.AuthenticationConfiguration.BruteForceProtectionConfiguration
import com.ruchij.config.development.ApplicationMode
import com.ruchij.types.FunctionKTypes.configReaderResultToIO
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
         |application-mode = "Local"
         |
         |http-configuration {
         |  port = 80
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
         |build-information {
         |  git-branch = "master"
         |  git-commit = "1234abc"
         |  build-timestamp = "$currentDateTime"
         |}
         |""".stripMargin

    val expectedServiceConfiguration =
      ServiceConfiguration(
        HttpConfiguration(port = 80),
        AuthenticationConfiguration(60 seconds, BruteForceProtectionConfiguration(10, 30 seconds)),
        ApplicationMode.Local,
        BuildInformation(Some("master"), Some("1234abc"), Some(currentDateTime))
      )

    ServiceConfiguration.load[IO](ConfigSource.string(configurationFile)).unsafeRunSync() mustBe expectedServiceConfiguration
  }
}
